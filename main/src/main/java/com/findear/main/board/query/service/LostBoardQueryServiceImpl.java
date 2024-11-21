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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class LostBoardQueryServiceImpl implements LostBoardQueryService {

    private final LostBoardQueryRepository lostBoardQueryRepository;
    private final int PAGE_SIZE = 10;

    private final LocalDate DEFAULT_SDATE = LocalDate.parse("2015-01-01");

    public LostBoardListResponse findAll(FindAllLostBoardReqDto findAllReq) {
        Pageable pageable = PageRequest.of(findAllReq.getPageNo() - 1, findAllReq.getSize());
        // 날짜 한쪽만 있는지 체크
        LocalDate sDate = findAllReq.getSDate();
        LocalDate eDate = findAllReq.getEDate();
        if (sDate != null && eDate == null) {
            findAllReq.setEDate(LocalDate.now());
        } else if (sDate == null && eDate != null) {
            findAllReq.setSDate(DEFAULT_SDATE);
        }

        Page<LostBoardListResDto> resultPage = lostBoardQueryRepository.findAllWithDtoForm(findAllReq, pageable);
        return new LostBoardListResponse(resultPage.getContent(), resultPage.getTotalPages());
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
