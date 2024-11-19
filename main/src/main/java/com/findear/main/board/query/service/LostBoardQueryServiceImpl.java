package com.findear.main.board.query.service;


import com.findear.main.board.common.domain.LostBoard;
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

    public LostBoardListResponse findAll(Long memberId, String category, String sDate, String eDate, String keyword,
                                         String sortBy, Boolean desc, int pageNo, int size) {
        List<LostBoard> lostBoards = null;
        if (sortBy != null && sortBy.equals("date")) {
            lostBoards = desc ? lostBoardQueryRepository.findAllOrderByLostAtDesc()
                    : lostBoardQueryRepository.findAllOrderByLostAt();
        } else {
            lostBoards = lostBoardQueryRepository.findAll();
        }
        Stream<LostBoard> stream = lostBoards.stream();

        // filtering
        if (memberId != null) {
            stream = stream.filter(lost -> {
                Long mId = lost.getBoard().getMember().getId();
                return mId != null && mId.equals(memberId);
            });
        }
        if (category != null) {
            stream = stream.filter(lost -> {
                String cName = lost.getBoard().getCategoryName();
                return cName != null && cName.contains(category);
            });
        }
        if (sDate != null || eDate != null) {
            stream = stream.filter(
                    lost -> lost.getLostAt() != null
                            && !lost.getLostAt().isBefore(sDate != null ? LocalDate.parse(sDate) : LocalDate.parse(DEFAULT_SDATE_STRING))
                            && !lost.getLostAt().isAfter(eDate != null ? LocalDate.parse(eDate) : LocalDate.now())
            );
        }
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
