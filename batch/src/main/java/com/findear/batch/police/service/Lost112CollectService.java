package com.findear.batch.police.service;

import com.findear.batch.police.client.Lost112ApiService;
import com.findear.batch.police.client.Lost112Client;
import com.findear.batch.police.client.Lost112Exception;
import com.findear.batch.police.client.Lost112Page;
import com.findear.batch.police.client.Lost112Properties;
import com.findear.batch.police.client.Lost112XmlParser;
import com.findear.batch.police.domain.PoliceAcquiredData;
import com.findear.batch.police.exception.Lost112NotConfiguredException;
import com.findear.batch.police.exception.Lost112UnavailableException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lost112 수집 (공식 명세 기준, 05 §3). 경찰청·포털기관 두 서비스를 차례로 돌며 <b>페이지마다</b>
 * 요청 → 파싱 → 정규화 → bulk 인덱싱을 하고 다음 페이지로 넘어간다 (앞 페이지 데이터를 들고 있지 않는다).
 * 문서 ID는 atcId라 다시 수집하면 덮어쓰고(upsert), 인덱스를 지우지 않는다.
 * 한 서비스가 실패하면 그 서비스만 거기서 멈추고(이미 넣은 문서는 유지) 다음 서비스는 계속한다.
 *
 * 수집 결과는 서비스(POLICE·PORTAL)마다 한 번씩 지표로 센다. policeJob은 수집이 실패해도 COMPLETED라(D-55) 잡 지표로는 보이지 않기 때문이다.
 * - findear.lost112.ingest.runs{service,result=success|failure} (Prometheus: findear_lost112_ingest_runs_total).
 *   서비스가 오류로 멈췄으면 일부 페이지를 넣었어도 failure다
 * - findear.lost112.ingest.items{service,outcome=indexed|skipped} (findear_lost112_ingest_items_total). 실패한 서비스가 멈추기 전에 넣은 문서도 센다
 * 키가 없어 요청을 보내지 않은 경우는 세지 않는다. 기동 때 모든 태그 조합을 0으로 미리 등록한다.
 */
@Slf4j
@Service
public class Lost112CollectService {

    static final String RUNS_METRIC = "findear.lost112.ingest.runs";
    static final String ITEMS_METRIC = "findear.lost112.ingest.items";

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

    private final Lost112Client client;
    private final Lost112XmlParser parser;
    private final PoliceDataNormalizer normalizer;
    private final PoliceAcquiredDataIndexer indexer;
    private final Lost112Properties properties;
    private final Map<Lost112ApiService, Counter> successRuns = new EnumMap<>(Lost112ApiService.class);
    private final Map<Lost112ApiService, Counter> failureRuns = new EnumMap<>(Lost112ApiService.class);
    private final Map<Lost112ApiService, Counter> indexedItems = new EnumMap<>(Lost112ApiService.class);
    private final Map<Lost112ApiService, Counter> skippedItems = new EnumMap<>(Lost112ApiService.class);

    public Lost112CollectService(Lost112Client client, Lost112XmlParser parser, PoliceDataNormalizer normalizer,
                                 PoliceAcquiredDataIndexer indexer, Lost112Properties properties, MeterRegistry meterRegistry) {
        this.client = client;
        this.parser = parser;
        this.normalizer = normalizer;
        this.indexer = indexer;
        this.properties = properties;
        for (Lost112ApiService service : Lost112ApiService.values()) {
            String name = service.name(); // POLICE, PORTAL
            successRuns.put(service, runs(meterRegistry, name, "success"));
            failureRuns.put(service, runs(meterRegistry, name, "failure"));
            indexedItems.put(service, items(meterRegistry, name, "indexed"));
            skippedItems.put(service, items(meterRegistry, name, "skipped"));
        }
    }

    private static Counter runs(MeterRegistry registry, String service, String result) {
        return Counter.builder(RUNS_METRIC)
                .description("Lost112 수집 실행 수 (서비스별, failure = 그 서비스가 오류로 멈춤)")
                .tag("service", service).tag("result", result)
                .register(registry);
    }

    private static Counter items(MeterRegistry registry, String service, String outcome) {
        return Counter.builder(ITEMS_METRIC)
                .description("Lost112 수집 item 수 (indexed = 인덱스에 넣음, skipped = 필수값 없음·형식 오류로 건너뜀)")
                .tag("service", service).tag("outcome", outcome)
                .register(registry);
    }

    /**
     * 수집을 실행하고 요약을 돌려준다. 서비스별 실패는 요약의 error에 담는다.
     *
     * @throws Lost112NotConfiguredException 키가 비어 있을 때 (요청을 보내지 않는다)
     */
    public Lost112CollectResult collect() {

        if (!properties.hasServiceKey()) {
            throw new Lost112NotConfiguredException();
        }

        LocalDate today = LocalDate.now(SEOUL);
        String startYmd = today.minusDays(properties.getCollectDays()).format(YMD);
        String endYmd = today.format(YMD);
        log.info("Lost112 수집 시작: {} ~ {}, 페이지 크기 {}", startYmd, endYmd, properties.getPageSize());

        List<Lost112CollectResult.ServiceResult> results = new ArrayList<>();
        for (Lost112ApiService service : Lost112ApiService.values()) {
            Lost112CollectResult.ServiceResult result = collectService(service, startYmd, endYmd);
            results.add(result);
            record(service, result);
            log.info("Lost112 수집 {}: pages={}, fetched={}, indexed={}, skipped={}, truncated={}, error={}",
                    service, result.pages(), result.fetched(), result.indexed(), result.skipped(), result.truncated(),
                    result.error() == null ? "-" : result.error());
        }

        try {
            indexer.refresh();
        } catch (RuntimeException e) {
            // 인덱스가 아직 없는 경우(문서를 하나도 못 넣음) 등. 수집 결과에는 영향이 없다
            log.debug("refresh 생략 ({})", e.getClass().getSimpleName());
        }
        return new Lost112CollectResult(startYmd, endYmd, results);
    }

    /** 수집하고, 실패만 있고 하나도 못 넣었으면 {@link Lost112UnavailableException}을 던진다 (수동 실행 API·잡용) */
    public Lost112CollectResult collectOrThrow() {

        Lost112CollectResult result = collect();
        if (result.totalFailure()) {
            throw new Lost112UnavailableException(result.failureSummary());
        }
        return result;
    }

    private void record(Lost112ApiService service, Lost112CollectResult.ServiceResult result) {
        (result.failed() ? failureRuns : successRuns).get(service).increment();
        indexedItems.get(service).increment(result.indexed());
        skippedItems.get(service).increment(result.skipped());
    }

    private Lost112CollectResult.ServiceResult collectService(Lost112ApiService service, String startYmd, String endYmd) {

        int pageSize = properties.getPageSize();
        int maxPages = properties.getMaxPages();
        int pages = 0;
        int fetched = 0;
        int indexed = 0;
        int skipped = 0;
        boolean truncated = false;

        try {
            for (int pageNo = 1; ; pageNo++) {

                if (pageNo > maxPages) {
                    truncated = true;
                    log.warn("Lost112 수집 {}: max-pages({})에 닿아 멈춥니다. 수집하지 못한 페이지가 남아 있을 수 있습니다", service, maxPages);
                    break;
                }

                Lost112Page page = parser.parse(client.fetchPage(service, pageNo, pageSize, startYmd, endYmd));
                pages++;
                fetched += page.items().size();

                List<PoliceAcquiredData> documents = new ArrayList<>();
                for (Map<String, String> item : page.items()) {
                    Optional<PoliceAcquiredData> document = normalizer.normalize(item, service);
                    if (document.isPresent()) {
                        documents.add(document.get());
                    } else {
                        skipped++;
                    }
                }
                indexer.index(documents);
                indexed += documents.size();
                log.debug("Lost112 수집 {} {}페이지: items={}, indexed={}", service, pageNo, page.items().size(), documents.size());

                if (isLastPage(page, pageNo, pageSize)) {
                    break;
                }
            }
            return new Lost112CollectResult.ServiceResult(service, pages, fetched, indexed, skipped, truncated, null);

        } catch (Lost112Exception e) {
            return new Lost112CollectResult.ServiceResult(service, pages, fetched, indexed, skipped, truncated, e.getMessage());
        } catch (RuntimeException e) {
            // 인덱싱·정규화 실패 등 (HTTP·파싱 오류는 위에서 키 없이 Lost112Exception으로 바뀐다).
            // 응답 error에는 종류만 담고(메시지에 문서 내용이 길게 들어갈 수 있음), 원인은 로그로 남긴다
            log.warn("Lost112 수집 {}: 처리 중 오류", service, e);
            return new Lost112CollectResult.ServiceResult(service, pages, fetched, indexed, skipped, truncated,
                    "처리 중 오류 (" + e.getClass().getSimpleName() + ")");
        }
    }

    /** 받은 item이 0개이거나 pageNo * numOfRows >= totalCount. totalCount가 응답에 없으면 빈 페이지가 나올 때까지 계속한다 */
    private boolean isLastPage(Lost112Page page, int pageNo, int pageSize) {

        if (page.items().isEmpty()) {
            return true;
        }
        return page.totalCount() != null && (long) pageNo * pageSize >= page.totalCount();
    }
}
