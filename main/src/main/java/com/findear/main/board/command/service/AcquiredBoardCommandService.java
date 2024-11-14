package com.findear.main.board.command.service;

import com.findear.main.board.command.dto.*;
import com.findear.main.board.command.repository.*;
import com.findear.main.board.common.domain.*;
import com.findear.main.board.common.dto.ScrapListResDto;
import com.findear.main.board.query.dto.AcquiredBoardListResDto;
import com.findear.main.board.query.dto.BatchServerResponseDto;
import com.findear.main.board.query.repository.AcquiredBoardQueryRepository;
import com.findear.main.board.query.repository.BoardQueryRepository;
import com.findear.main.member.common.domain.Agency;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.query.service.MemberQueryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class AcquiredBoardCommandService {

    private final AcquiredBoardCommandRepository acquiredBoardCommandRepository;
    private final AcquiredBoardQueryRepository acquiredBoardQueryRepository;
    private final BoardCommandRepository boardCommandRepository;
    private final BoardQueryRepository boardQueryRepository;
    private final MemberQueryService memberQueryService;
    private final ImgFileRepository imgFileRepository;
    private final ReturnLogRepository returnLogRepository;
    private final ScrapRepository scrapRepository;
    private final Lost112ScrapRepository lost112ScrapRepository;
    private final RestTemplate restTemplate;

    @Value("${servers.match-server.url}")
    private String MATCH_SERVER_URL;
    @Value("${servers.batch-server.url}")
    private String BATCH_SERVER_URL;

    public Long register(PostAcquiredBoardReqDto postAcquiredBoardReqDto) {
        Member manager = memberQueryService.internalFindById(postAcquiredBoardReqDto.getMemberId());
        Board savedBoard = boardCommandRepository.save(Board.builder()
                .productName(postAcquiredBoardReqDto.getProductName())
                .member(manager)
                .thumbnailUrl(postAcquiredBoardReqDto.getImgUrls().get(0))
                .isLost(false)
                .status(BoardStatus.ONGOING)
                .build());

        List<ImgFile> imgFiles = new ArrayList<>();
        for (String imgUrl : postAcquiredBoardReqDto.getImgUrls()) {
            ImgFile imgFile = new ImgFile(savedBoard, imgUrl);
            ImgFile savedFile = imgFileRepository.save(imgFile);
            imgFiles.add(savedFile);
        }
        savedBoard.updateImgFiles(imgFiles);
        Agency agency = manager.getAgency();
        AcquiredBoard acquiredBoard = AcquiredBoard.builder()
                .board(savedBoard)
                .address(agency.getAddress())
                .name(agency.getName())
                .xPos(agency.getXPos())
                .yPos(agency.getYPos())
                .build();
        AcquiredBoard savedAcquiredBoard = acquiredBoardCommandRepository.save(acquiredBoard);

        // 비동기 처리됨
        sendAutoFillRequest(savedAcquiredBoard)
                .subscribe(
                        response -> fillColumns(savedAcquiredBoard, response),
                        error -> log.error("습득물 컬럼 자동 업데이트 실패. \nerror = " + error)
                );

        return savedBoard.getId();
    }

    /**
     * @param modifyReqDto
     * @return boardId
     */
    public Long modify(ModifyAcquiredBoardReqDto modifyReqDto) {
        AcquiredBoard acquiredBoard = acquiredBoardQueryRepository.findByBoardId(modifyReqDto.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다."));

        if (modifyReqDto.getImgUrls() != null && !modifyReqDto.getImgUrls().isEmpty()) {
            List<ImgFile> imgFileList = modifyReqDto.getImgUrls().stream()
//                .map(imgUrl -> imgFileRepository.findByImgUrl(imgUrl)
                    .map(imgUrl -> imgFileRepository.findFirstByImgUrl(imgUrl) // 개발환경용
                            .orElse(imgFileRepository.save(new ImgFile(acquiredBoard.getBoard(), imgUrl)))
                    ).toList();

            modifyReqDto.setImgFileList(imgFileList);
        }
        acquiredBoard.modify(modifyReqDto);

        return acquiredBoard.getBoard().getId();
    }

    private Mono<ModelServerResponseDto> sendAutoFillRequest(AcquiredBoard notFilledBoard) {
        WebClient client = WebClient.builder()
                .baseUrl(MATCH_SERVER_URL)
                .build();

        NotFilledBoardDto notFilledBoardDto = NotFilledBoardDto.of(notFilledBoard);

        WebClient.RequestHeadersSpec<?> requestHeadersSpec = client
                .post()
                .uri("/process")
                .bodyValue(notFilledBoardDto);
        Mono<ModelServerResponseDto> autofillReqMono = requestHeadersSpec
                .retrieve()
                .bodyToMono(ModelServerResponseDto.class);
        return autofillReqMono;
    }

    private void fillColumns(AcquiredBoard notFilledBoard, ModelServerResponseDto modelServerResponseDto) {
        log.info("modelServerResponse = " + modelServerResponseDto);
        notFilledBoard.updateAutoFilledColumn(modelServerResponseDto.getResult());
        boardCommandRepository.save(notFilledBoard.getBoard());
    }

    public void remove(Long boardId, Long memberId) {
        Board board = boardQueryRepository.findByIdAndDeleteYnFalse(boardId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시물이 없습니다."));
        if (!board.getMember().getId().equals(memberId)) {
            throw new AuthorizationServiceException("권한이 없습니다.");
        }
        board.remove();
    }

    public void giveBack(GiveBackReqDto giveBackReqDto) {
        AcquiredBoard acquiredBoard = acquiredBoardQueryRepository.findByBoardId(giveBackReqDto.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException("해당 게시물이 없습니다."));
        Board board = acquiredBoard.getBoard();
        if (board.getDeleteYn()) {
            throw new IllegalArgumentException("해당 게시물이 없습니다.");
        }
        // 같은 시설 관리자만 인계처리 가능
        checkSameAgency(board, giveBackReqDto.getManagerId());

        if (!board.getStatus().equals(BoardStatus.ONGOING)) {
            throw new IllegalArgumentException("이미 인계처리된 게시물입니다.");
        }

        ReturnLog savedLog = returnLogRepository.save(
                new ReturnLog(acquiredBoard, giveBackReqDto.getPhoneNumber())); // 비회원인 경우도 있음.
        acquiredBoard.getReturnLogList().add(savedLog);
        board.giveBack();
    }

    public void cancelGiveBack(Long managerId, Long boardId) {
        AcquiredBoard acquiredBoard = acquiredBoardQueryRepository.findByBoardId(boardId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시물이 없습니다."));
        Board board = acquiredBoard.getBoard();
        if (board.getDeleteYn()) {
            throw new IllegalArgumentException("해당 게시물이 없습니다.");
        }

        checkSameAgency(board, managerId);

        ReturnLog lastLog = returnLogRepository.findFirstByAcquiredBoardAndCancelAtIsNullOrderByIdDesc(acquiredBoard)
                .orElseThrow(() -> new IllegalArgumentException("잘못된 접근입니다."));
        // cancel
        lastLog.rollback();
        acquiredBoard.rollback();
    }

    public void scrap(Long memberId, String boardId, Boolean isFindear) {
        Member member = memberQueryService.internalFindById(memberId);
        if (isFindear) {
            Board board = boardQueryRepository.findByIdAndDeleteYnFalse(Long.parseLong(boardId))
                    .orElseThrow(() -> new IllegalArgumentException("해당 게시물이 없습니다."));
            if (scrapRepository.findByMemberAndBoard(member, board).isPresent()) {
                throw new IllegalArgumentException("이미 스크랩한 게시물입니다.");
            }
            scrapRepository.save(new Scrap(board, member));
        } else {
            lost112ScrapRepository.save(new Lost112Scrap(boardId, member)); // atcId
        }
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

    public void cancelScrap(Long memberId, String boardId, Boolean isFindear) {
        Member member = memberQueryService.internalFindById(memberId);
        if (isFindear) {
            Board board = boardQueryRepository.findByIdAndDeleteYnFalse(Long.parseLong(boardId))
                    .orElseThrow(() -> new IllegalArgumentException("해당 게시물이 없습니다."));

            Scrap scrap = scrapRepository.findByMemberAndBoard(member, board)
                    .orElseThrow(() -> new IllegalArgumentException("잘못된 접근입니다."));

            scrapRepository.delete(scrap);
        } else {
            Lost112Scrap scrap = lost112ScrapRepository.findByMemberAndLost112AtcId(member, boardId)
                    .orElseThrow(() -> new IllegalArgumentException("잘못된 접근입니다."));
            lost112ScrapRepository.delete(scrap);
        }
    }

    private void checkSameAgency(Board board, Long memberId) {
        Agency writersAgency = board.getMember().getAgency();
        if (writersAgency == null) {
            return; // Member의 Agency도 로그로 남기지 않는 한, 비교 불가능한 경우
        }
        Long writersAgencyId = writersAgency.getId();
        Member requester = memberQueryService.internalFindById(memberId);
        Agency agency = requester.getAgency();
        if (agency == null || !writersAgencyId.equals(agency.getId())) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
    }
}
