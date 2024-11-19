package com.findear.main.board.query.service;

import com.findear.main.board.command.repository.Lost112ScrapRepository;
import com.findear.main.board.command.repository.ReturnLogRepository;
import com.findear.main.board.command.repository.ScrapRepository;
import com.findear.main.board.common.domain.AcquiredBoard;
import com.findear.main.board.common.domain.Lost112Scrap;
import com.findear.main.board.common.domain.Scrap;
import com.findear.main.board.common.dto.Lost112AcquiredBoardDto;
import com.findear.main.board.common.dto.ScrapListResDto;
import com.findear.main.board.query.dto.*;
import com.findear.main.board.query.repository.AcquiredBoardQueryRepository;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.query.service.MemberQueryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class AcquiredBoardQueryServiceImpl implements AcquiredBoardQueryService {

    private final AcquiredBoardQueryRepository acquiredBoardQueryRepository;
    private final ReturnLogRepository returnLogRepository;
    private final String DEFAULT_SDATE_STRING = "2015-01-01";
    private final RestTemplate restTemplate;

    @Value("${servers.batch-server.url}")
    private String BATCH_SERVER_URL;
    private Lost112ScrapRepository lost112ScrapRepository;
    private MemberQueryService memberQueryService;
    private ScrapRepository scrapRepository;

    public AcquiredBoardListResponse findAll(Long memberId, String category, String sDate, String eDate, String keyword,
                                             String sortBy, Boolean desc, int pageNo, int pageSize) {
        List<AcquiredBoard> acquiredBoards = null;
        if (sortBy != null && sortBy.equals("date")) {
            acquiredBoards = desc ? acquiredBoardQueryRepository.findAllOrderByAcquiredAtDesc()
                    : acquiredBoardQueryRepository.findAllOrderByAcquiredAt();
        } else {
            acquiredBoards = desc ? acquiredBoardQueryRepository.findAllDesc() : acquiredBoardQueryRepository.findAll();
        }
        Stream<AcquiredBoard> stream = acquiredBoards.stream();

        // filtering
        if (memberId != null) {
            stream = stream.filter(acquired -> {
                Long mId = acquired.getBoard().getMember().getId();
                return mId != null && mId.equals(memberId);
            });
        }

        if (category != null) {
            stream = stream.filter(acquired -> {
                String cName = acquired.getBoard().getCategoryName();
                return cName != null && cName.contains(category);
            });
        }
        if (sDate != null || eDate != null) {
            stream = stream.filter(
                    acquired -> acquired.getAcquiredAt() != null
                            && !acquired.getAcquiredAt().isBefore(sDate != null ? LocalDate.parse(sDate) : LocalDate.parse(DEFAULT_SDATE_STRING))
                            && !acquired.getAcquiredAt().isAfter(eDate != null ? LocalDate.parse(eDate) : LocalDate.now())
            );
        }
        if (keyword != null) {
            stream = stream.filter(acquired -> {
                String pName = acquired.getBoard().getProductName();
                return (pName != null && pName.contains(keyword))
                        || (acquired.getAddress() != null && acquired.getAddress().contains(keyword))
                        || (acquired.getName() != null && acquired.getName().contains(keyword));
            });
        }

        List<AcquiredBoardListResDto> filtered = stream
                .map(AcquiredBoardListResDto::of)
                .toList();

        // paging
        int eIdx = pageSize * pageNo;
        int sIdx = eIdx - pageSize;
        if (sIdx >= filtered.size()) return null;
        return new AcquiredBoardListResponse(filtered.subList(sIdx, Math.min(eIdx, filtered.size())),
                filtered.size() / pageSize + (filtered.size() % pageSize != 0 ? 1 : 0));
    }

    public List<?> findAllInLost112(String category, String sDate, String eDate, String keyword, int pageNo,
                                    int pageSize) {
        log.info("service 메소드 들어옴");
        if (sDate != null && eDate == null) {
            eDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        } else if (sDate == null && eDate != null) {
            sDate = LocalDate.parse(eDate).minusMonths(6)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        // request to batch server
        try {
            StringBuilder uriBuilder = new StringBuilder(BATCH_SERVER_URL);
            uriBuilder.append("?page=").append(pageNo)
                    .append("&size=").append(pageSize);

            if (category != null) {
                uriBuilder.append("&category=").append(category);
            }
            if (sDate != null) {
                uriBuilder.append("&startDate=").append(sDate)
                        .append("&endDate=").append(eDate);
            }
            if (keyword != null) {
                uriBuilder.append("&keyword=").append(keyword);
            }
            log.info("조회 파라미터(쿼리스트링) 세팅 끝");

            BatchServerResponseDto responseDto = restTemplate.getForObject(uriBuilder.toString(), BatchServerResponseDto.class);
            log.info(uriBuilder.toString());
            log.info("조회 결과 : " + responseDto);
            List<Lost112AcquiredBoardDto> result = (List<Lost112AcquiredBoardDto>) responseDto.getResult();

            return result; // 최신순 정렬된 데이터
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("필터링 변수 세팅 중 오류");
        }
    }

    public AcquiredBoardDetailResDto findById(Long acquiredBoardId) {
        AcquiredBoard acquiredBoard = acquiredBoardQueryRepository.findById(acquiredBoardId)
                .orElseThrow(() -> new IllegalArgumentException("없는 게시물입니다."));

        return AcquiredBoardDetailResDto.of(acquiredBoard);
    }

    public AcquiredBoardDetailResDto findByBoardId(Long boardId) {
        AcquiredBoard acquiredBoard = acquiredBoardQueryRepository.findByBoardId(boardId)
                .orElseThrow(() -> new IllegalArgumentException("없는 게시물입니다."));

        return AcquiredBoardDetailResDto.of(acquiredBoard);
    }

    public Integer getLost112TotalPageNum(int pageSize) {
        BatchServerResponseDto response = restTemplate.getForObject(BATCH_SERVER_URL + "/total", BatchServerResponseDto.class);
        Integer totalRowNum = (Integer) response.getResult();
        return Math.max(1, totalRowNum / pageSize + (totalRowNum % pageSize == 0 ? 0 : 1));
    }

    public Map<String, Long> getYesterdaysReturnCount() {
        Map<String, Long> result = new HashMap<>();

        result.put("yesterday", returnLogRepository.countReturn(LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)));
        result.put("today", returnLogRepository.countReturn(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)));
        return result;
    }

    public ScrapListResDto findScrapList(Long memberId) {
        // findear
        Member member = memberQueryService.internalFindById(memberId);
        List<Scrap> myScraps = scrapRepository.findAllByMember(member);
        List<AcquiredBoardListResDto> findearAcquireds = new ArrayList<>(myScraps.size());
        for (Scrap scrap : myScraps) {
            AcquiredBoard acquiredBoard = acquiredBoardQueryRepository.findByBoardId(scrap.getBoard().getId())
                    .orElseThrow(() -> new IllegalArgumentException("오류 : 없는 게시물이 스크랩됨"));
            findearAcquireds.add(AcquiredBoardListResDto.of(acquiredBoard));
        }

        // lost112
        List<Lost112Scrap> lost112Scraps = lost112ScrapRepository.findAllByMember(member);
        List<String> atcIdList = lost112Scraps.stream()
                .map(Lost112Scrap::getLost112AtcId)
                .toList();
        BatchServerResponseDto response = restTemplate.postForObject(BATCH_SERVER_URL + "/police/scrap",
                atcIdList, BatchServerResponseDto.class);
        List<Map<String, Object>> lost112Acquireds = (List<Map<String, Object>>) response.getResult();
        return new ScrapListResDto(findearAcquireds, lost112Acquireds);
    }
}
