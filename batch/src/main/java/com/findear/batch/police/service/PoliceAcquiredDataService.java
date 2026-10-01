package com.findear.batch.police.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.findear.batch.common.elasticsearch.ElasticsearchSourceReader;
import com.findear.batch.police.domain.PoliceAcquiredData;
import com.findear.batch.police.exception.PoliceException;
import com.findear.batch.police.repository.PoliceAcquiredDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class PoliceAcquiredDataService {

    private static final String INDEX = "police_acquired_data";

    private static final String[] SOURCE_FIELDS = {"id", "atcId", "depPlace", "fdFilePathImg", "fdPrdtNm",
            "fdSbjt", "clrNm", "fdYmd", "prdtClNm", "mainPrdtClNm", "subPrdtClNm"};

    private final PoliceAcquiredDataRepository policeAcquiredDataRepository;
    private final ElasticsearchSourceReader sourceReader;

    @Value("${lost112.service-key}")
    private String secretKey;

    @Value("${lost112.base-url}")
    private String lost112BaseUrl;

    public void deleteDatas() {

        policeAcquiredDataRepository.deleteAll();
    }


    public List<PoliceAcquiredData> search(int page, int size, String category,
                                           String startDate, String endDate, String keyword) {

        log.info("page = " + page);
        log.info("size = " + size);
        log.info("category = " + category);
        log.info("startDate = " + startDate);
        log.info("endDate = " + endDate);
        log.info("keyword = " + keyword);

        try {
            List<PoliceAcquiredData> allDatas = new ArrayList<>();

            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            // category가 제공되었을 경우
            if (category != null && !category.isEmpty()) {
                boolQuery.must(m -> m.match(mm -> mm.field("mainPrdtClNm").query(category)));
            }

            // fdYmd는 ES가 날짜(date)로 매핑한 필드라 yyyy-MM-dd 문자열로 범위를 건다 (lte는 그 날 끝까지 포함)
            if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
                // startDate와 endDate가 모두 제공되었을 경우
                String start = LocalDate.parse(startDate).toString();
                String end = LocalDate.parse(endDate).toString();
                boolQuery.filter(f -> f.range(r -> r.date(d -> d.field("fdYmd").gte(start).lte(end))));
            } else if (startDate != null && !startDate.isEmpty()) {
                // startDate만 제공되는 경우
                String start = LocalDate.parse(startDate).toString();
                boolQuery.filter(f -> f.range(r -> r.date(d -> d.field("fdYmd").gte(start))));
            } else if (endDate != null && !endDate.isEmpty()) {
                // endDate만 제공되는 경우
                String end = LocalDate.parse(endDate).toString();
                boolQuery.filter(f -> f.range(r -> r.date(d -> d.field("fdYmd").lte(end))));
            } else {
                // startDate와 endDate가 모두 없는 경우 기본값으로 오늘까지
                String today = LocalDate.now().toString();
                boolQuery.filter(f -> f.range(r -> r.date(d -> d.field("fdYmd").lte(today))));
            }

            // keyword가 제공되었을 경우
            if (keyword != null && !keyword.isEmpty()) {
                boolQuery.must(m -> m.match(mm -> mm.field("fdSbjt").query(keyword)));
            }

            // 페이지 번호와 사이즈에 따라 검색 시작 위치(from)와 건수(size)를 ES에서 자른다
            NativeQuery query = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.bool(boolQuery.build())))
                    .withPageable(PageRequest.of(page - 1, size))
                    .withSourceFilter(new FetchSourceFilterBuilder().withIncludes(SOURCE_FIELDS).build())
                    .build();

            for (Map<String, Object> source : sourceReader.search(INDEX, query)) {
                allDatas.add(convertToPoliceData(source));
            }

            return allDatas;

        } catch (Exception e) {
            throw new PoliceException(e.getMessage());
        }
    }


    public List<PoliceAcquiredData> searchAllDatas() {
        try {
            List<PoliceAcquiredData> allDatas = new ArrayList<>();

            // 전체를 scroll로 500건씩 읽는다
            NativeQuery query = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.matchAll(m -> m)))
                    .withPageable(PageRequest.of(0, 500))
                    .withSourceFilter(new FetchSourceFilterBuilder().withIncludes(SOURCE_FIELDS).build())
                    .build();

            for (Map<String, Object> source : sourceReader.searchAll(INDEX, query)) {
                allDatas.add(convertToPoliceData(source));
            }

            return allDatas;

        } catch (Exception e) {

            e.printStackTrace();
        }
        return null;
    }

    private PoliceAcquiredData convertToPoliceData(Map<String, Object> sourceAsMap) {

        if(sourceAsMap.get("fdSbjt") == null) {

            return new PoliceAcquiredData(
                    Long.parseLong(sourceAsMap.get("id").toString()),
                    sourceAsMap.get("atcId").toString(),
                    sourceAsMap.get("depPlace").toString(),
                    sourceAsMap.get("fdFilePathImg").toString(),
                    sourceAsMap.get("fdPrdtNm").toString(),
                    sourceAsMap.get("clrNm").toString(),
                    sourceAsMap.get("fdYmd").toString(),
                    sourceAsMap.get("prdtClNm").toString(),
                    sourceAsMap.get("mainPrdtClNm").toString(),
                    sourceAsMap.getOrDefault("subPrdtClNm", "").toString()
            );
        } else if(sourceAsMap.get("clrNm") == null) {

            return new PoliceAcquiredData(

                    Long.parseLong(sourceAsMap.get("id").toString()),
                    sourceAsMap.get("atcId").toString(),
                    sourceAsMap.get("depPlace").toString(),
                    sourceAsMap.get("fdFilePathImg").toString(),
                    sourceAsMap.get("fdPrdtNm").toString(),
                    sourceAsMap.get("fdSbjt").toString(),
                    sourceAsMap.get("fdYmd").toString(),
                    sourceAsMap.get("prdtClNm").toString(),
                    sourceAsMap.get("mainPrdtClNm").toString(),
                    sourceAsMap.getOrDefault("subPrdtClNm", "").toString()
            );
        } else if(sourceAsMap.get("depPlace") == null) {

            return new PoliceAcquiredData(

                    Long.parseLong(sourceAsMap.get("id").toString()),
                    sourceAsMap.get("atcId").toString(),
                    sourceAsMap.get("fdFilePathImg").toString(),
                    sourceAsMap.get("fdPrdtNm").toString(),
                    sourceAsMap.get("fdSbjt").toString(),
                    sourceAsMap.get("clrNm").toString(),
                    sourceAsMap.get("fdYmd").toString(),
                    sourceAsMap.get("prdtClNm").toString(),
                    sourceAsMap.get("mainPrdtClNm").toString(),
                    sourceAsMap.getOrDefault("subPrdtClNm", "").toString()
            );
        }
        else {

            return new PoliceAcquiredData(

                    Long.parseLong(sourceAsMap.get("id").toString()),
                    sourceAsMap.get("atcId").toString(),
                    sourceAsMap.get("depPlace").toString(),
                    sourceAsMap.get("fdFilePathImg").toString(),
                    sourceAsMap.get("fdPrdtNm").toString(),
                    sourceAsMap.get("fdSbjt").toString(),
                    sourceAsMap.get("clrNm").toString(),
                    sourceAsMap.get("fdYmd").toString(),
                    sourceAsMap.get("prdtClNm").toString(),
                    sourceAsMap.get("mainPrdtClNm").toString(),
                    sourceAsMap.getOrDefault("subPrdtClNm", "").toString()
            );
        }

    }

    public Page<PoliceAcquiredData> searchByPage(int page, int size) {

        return policeAcquiredDataRepository.findAll(PageRequest.of(page, size));
    }


    public void savePoliceData() {

        // 키가 없으면 외부 API 요청도, 기존 데이터 삭제도 하지 않는다
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("LOST112_SERVICE_KEY가 설정되지 않았습니다");
        }

        try {

            // elastic search 모든 데이터 삭제
            deleteDatas();
            log.info("데이터 삭제 성공");

            String startDate = "20240101";
            LocalDateTime today = LocalDateTime.now();
            String todaysDate = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            log.info(todaysDate + "까지 데이터 저장");

            String numOfRows = "30000";

            BufferedReader rd;

            List<String> responses = new ArrayList<>();

            // 경찰청 산하 습득물 데이터 처리
            int count = 0;
            for(int pageNo = 1; ; pageNo++) {

                /*URL*/
                String urlBuilder = lost112BaseUrl + "/LosPtfundInfoInqireService/getPtLosfundInfoAccToClAreaPd" + "?" + URLEncoder.encode("serviceKey", StandardCharsets.UTF_8) + "=" + secretKey + /*Service Key*/
                        "&" + URLEncoder.encode("pageNo", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(String.valueOf(pageNo), StandardCharsets.UTF_8) + /*페이지번호*/
                        "&" + URLEncoder.encode("numOfRows", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(numOfRows, StandardCharsets.UTF_8) + /*한 페이지 결과 수*/
                        "&" + URLEncoder.encode("PRDT_CL_CD_01", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("", StandardCharsets.UTF_8) + /*대분류*/
                        "&" + URLEncoder.encode("PRDT_CL_CD_02", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("", StandardCharsets.UTF_8) + /*중분류*/
                        "&" + URLEncoder.encode("CLR_CD", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("", StandardCharsets.UTF_8) + /*습득물 색상*/
                        "&" + URLEncoder.encode("START_YMD", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(startDate, StandardCharsets.UTF_8) + /*검색시작일*/
                        "&" + URLEncoder.encode("END_YMD", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(todaysDate, StandardCharsets.UTF_8) + /*검색종료일*/
                        "&" + URLEncoder.encode("N_FD_LCT_CD", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("", StandardCharsets.UTF_8); /*습득지역*/

                URL url = new URL(urlBuilder);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-type", "application/json");

                System.out.println("Response code: " + conn.getResponseCode());

                if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
                    rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                boolean isEnd = false;
                String line;
                while ((line = rd.readLine()) != null) {

                    if(!(line.contains("atcId")) && count == 0) {
                        count++;
                        continue;
                    }

                    if(!(line.contains("atcId")) && count == 1) {
                        isEnd = true;
                        break;
                    }

                    log.info(line);

                    responses.add(line);
                }

                rd.close();
                conn.disconnect();

                if(isEnd) {
                    break;
                }
            }


            // 경찰청 습득물 데이터 처리
            count = 0;
            for(int pageNo = 1; ; pageNo++) {

                /*URL*/
                String urlBuilder = lost112BaseUrl + "/LosfundInfoInqireService/getLosfundInfoAccToClAreaPd" + "?" + URLEncoder.encode("serviceKey", StandardCharsets.UTF_8) + "=" + secretKey + /*Service Key*/
                        "&" + URLEncoder.encode("pageNo", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(String.valueOf(pageNo), StandardCharsets.UTF_8) + /*페이지번호*/
                        "&" + URLEncoder.encode("numOfRows", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(numOfRows, StandardCharsets.UTF_8) + /*한 페이지 결과 수*/
                        "&" + URLEncoder.encode("PRDT_CL_CD_01", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("", StandardCharsets.UTF_8) + /*대분류*/
                        "&" + URLEncoder.encode("PRDT_CL_CD_02", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("", StandardCharsets.UTF_8) + /*중분류*/
                        "&" + URLEncoder.encode("CLR_CD", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("", StandardCharsets.UTF_8) + /*습득물 색상*/
                        "&" + URLEncoder.encode("START_YMD", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(startDate, StandardCharsets.UTF_8) + /*검색시작일*/
                        "&" + URLEncoder.encode("END_YMD", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(todaysDate, StandardCharsets.UTF_8) + /*검색종료일*/
                        "&" + URLEncoder.encode("N_FD_LCT_CD", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("", StandardCharsets.UTF_8); /*습득지역*/

                URL url = new URL(urlBuilder);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-type", "application/json");

                System.out.println("Response code: " + conn.getResponseCode());

                if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
                    rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                boolean isEnd = false;
                String line;
                while ((line = rd.readLine()) != null) {

                    if(!(line.contains("atcId")) && count == 0) {
                        count++;
                        continue;
                    }

                    if(!(line.contains("atcId")) && count == 1) {
                        isEnd = true;
                        break;
                    }

                    log.info(line);

                    responses.add(line);
                }


                rd.close();
                conn.disconnect();

                if(isEnd) {
                    break;
                }
            }

            // 업로드 및 lastTrs 업데이트 실행
            readandSaveData(responses);

        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


    /**
     * 각 속성값 문자열로 추출
     * @param responses
     * @return
     */
    private void readandSaveData(List<String> responses) {

        try {

            Long id = 1L;

            List<PoliceAcquiredData> policeAcquiredDataList = new ArrayList<>();

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();

            for (String response : responses) {

                // String -> document 변환
                Document xmlDoc = builder.parse(new ByteArrayInputStream(response.getBytes()));
                xmlDoc.getDocumentElement().normalize();

                Element root = xmlDoc.getDocumentElement();
                NodeList items = root.getElementsByTagName("item");

                // 데이터 읽어오기
                int length = items.getLength();

                String[] targetTagNames = {"atcId", "depPlace", "fdFilePathImg",
                        "fdPrdtNm", "fdSbjt", "fdYmd", "prdtClNm"};

                for (int i = 0; i < length; i++) {

                    PoliceAcquiredData policeAcquiredData;

                    Element item = (Element) items.item(i);
                    String[] rowData = new String[7];


                    for (int j = 0; j < targetTagNames.length; j++) {
                        Node valueNode = item.getElementsByTagName(targetTagNames[j]).item(0);
                        String value = (valueNode == null) ? null : valueNode.getTextContent();

                        // null값 있는(잘못된) 데이터는 pass
                        if (value == null) continue;

                        rowData[j] = value.trim();

                    }

                    String[] array;
                    String fdSbjt = "";
                    String clrNm = "";


                    // 색상 컬럼 분류 로직
                    if(rowData[4] != null) {
                        fdSbjt = rowData[4];
                        System.out.println("설명 : " + fdSbjt);

                        // '색'이란 단어를 기준으로 문자열 분할
                        String[] parts = rowData[4].split("색");

                        if(parts.length == 1) {
                            System.out.println("색이란 단어가 없음");
                            clrNm = null;
                        }
                        else {

                            // 분할된 문자열 중 마지막 부분을 선택하여 '색상' 추출
                            String lastPart = parts[parts.length - 2];
                            System.out.println("lastPart : " + lastPart);

                            List<Integer> indexs = new ArrayList<>();
                            for(int j=0; j<lastPart.length(); j++) {
                                if(lastPart.charAt(j) == '(') {
                                    indexs.add(j);
                                }
                            }

                            if(indexs.size() < 2) {

                                clrNm = null;
                            } else {

//                                String color = lastPart.substring(indexs.get(indexs.size()-2) + 1, indexs.get(indexs.size()-1));
                                String color;
                                try {

                                    color = lastPart.substring(indexs.get(indexs.size()-1) + 1, lastPart.length()-1);
                                    clrNm = color;
                                    System.out.println("color : " + color);
                                } catch (StringIndexOutOfBoundsException e) {
                                    color = null;
                                    System.out.println("color : " + color);
                                }
                            }
                        }
                    }


                    // 대분류, 소분류 컬럼 분류 로직
                    String prdtClNm = rowData[6];
                    array = prdtClNm.split(" ");
                    String mainPrdtClNm = array[0];

                    String subPrdtClNm;
                    if(array.length != 3) {
                        subPrdtClNm = null;
                    }
                    else {

                        subPrdtClNm = array[2];
                    }

                    policeAcquiredData = new PoliceAcquiredData(id++,
                            rowData[0], rowData[1], rowData[2], rowData[3], rowData[4],
                            clrNm, rowData[5], rowData[6], mainPrdtClNm, subPrdtClNm);

                    policeAcquiredDataList.add(policeAcquiredData);

                }

                policeAcquiredDataRepository.saveAll(policeAcquiredDataList);

                policeAcquiredDataList.clear();

                log.info("데이터 저장 완료");

            }

            log.info("모든 데이터 저장 완료");

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public long getTotalCount() {

        return policeAcquiredDataRepository.count();
    }

}
