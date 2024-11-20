package com.findear.main.board.query.service;


import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.board.query.dto.FindAllLostBoardReqDto;
import com.findear.main.board.query.dto.LostBoardDetailResDto;
import com.findear.main.board.query.dto.LostBoardListResDto;
import com.findear.main.board.query.dto.LostBoardListResponse;
import com.findear.main.board.query.repository.LostBoardQueryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class LostBoardQueryServiceImpl implements LostBoardQueryService {

    private final LostBoardQueryRepository lostBoardQueryRepository;
    private final int PAGE_SIZE = 10;

    private final String DEFAULT_SDATE_STRING = "2015-01-01";

    // FIXME: LostBoard 조회 시 Member, AcquiredBoard, Notification 추가로 조회되는 문제 수정
    public LostBoardListResponse findAll(FindAllLostBoardReqDto findAllReq) {
        List<LostBoard> lostBoards = null;
        if (findAllReq.getSortBy() != null && findAllReq.getSortBy().equals("date")) {
            lostBoards = findAllReq.isDesc() ? lostBoardQueryRepository.findAllOrderByLostAtDesc()
                    : lostBoardQueryRepository.findAllOrderByLostAt();
        } else {
            lostBoards = lostBoardQueryRepository.findAll();
        }
        Stream<LostBoard> stream = lostBoards.stream();

        // filtering
        Long memberId = findAllReq.getMemberId();
        if (memberId != null) {
            stream = stream.filter(lost -> {
                Long mId = lost.getBoard().getMember().getId();
                return mId != null && mId.equals(memberId);
            });
        }
        String category = findAllReq.getCategory();
        if (category != null) {
            stream = stream.filter(lost -> {
                String cName = lost.getBoard().getCategoryName();
                return cName != null && cName.contains(category);
            });
        }
        String sDate = findAllReq.getSDate();
        String eDate = findAllReq.getEDate();
        if (sDate != null || eDate != null) {
            stream = stream.filter(
                    lost -> lost.getLostAt() != null
                            && !lost.getLostAt().isBefore(sDate != null ? LocalDate.parse(sDate) : LocalDate.parse(DEFAULT_SDATE_STRING))
                            && !lost.getLostAt().isAfter(eDate != null ? LocalDate.parse(eDate) : LocalDate.now())
            );
        }
        String keyword = findAllReq.getKeyword();
        if (keyword != null) {
            stream = stream.filter(lost -> {
                String pName = lost.getBoard().getProductName();
                return (pName != null && pName.contains(keyword))
                        || (lost.getSuspiciousPlace() != null
                        && lost.getSuspiciousPlace().contains(keyword));
            });
        }

        List<LostBoardListResDto> filtered = stream
                .map(LostBoardListResDto::of)
                .toList();

        // paging
        int size = findAllReq.getSize();
        int pageNo = findAllReq.getPageNo();

        int eIdx = size * pageNo;
        int sIdx = eIdx - size;
        if (sIdx >= filtered.size()) return null;
        return new LostBoardListResponse(filtered.subList(sIdx, Math.min(eIdx, filtered.size())),
                filtered.size() / size + (filtered.size() % size != 0 ? 1 : 0));
    }

    public LostBoardDetailResDto findById(Long lostBoardId) {
        LostBoard lostBoard = lostBoardQueryRepository.findById(lostBoardId)
                .orElseThrow(() -> new IllegalArgumentException("없는 게시물입니다."));

        return LostBoardDetailResDto.of(lostBoard);
    }

    public LostBoardDetailResDto findByBoardId(Long boardId) {
        LostBoard lostBoard = lostBoardQueryRepository.findByBoardId(boardId)
                .orElseThrow(() -> new IllegalArgumentException("없는 게시물입니다."));

        return LostBoardDetailResDto.of(lostBoard);
    }
}
