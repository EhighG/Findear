package com.findear.main.board.command.service;

import com.findear.main.board.command.dto.*;
import com.findear.main.board.command.repository.*;
import com.findear.main.board.common.domain.*;
import com.findear.main.board.query.repository.AcquiredBoardQueryRepository;
import com.findear.main.board.query.repository.BoardQueryRepository;
import com.findear.main.member.common.domain.Agency;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.query.service.MemberQueryService;
import com.findear.main.storage.ImageStorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class AcquiredBoardCommandServiceImpl implements AcquiredBoardCommandService {

    private final AcquiredBoardCommandRepository acquiredBoardCommandRepository;
    private final AcquiredBoardQueryRepository acquiredBoardQueryRepository;
    private final BoardCommandRepository boardCommandRepository;
    private final BoardQueryRepository boardQueryRepository;
    private final MemberQueryService memberQueryService;
    private final ImgFileRepository imgFileRepository;
    private final ReturnLogRepository returnLogRepository;
    private final ScrapRepository scrapRepository;
    private final Lost112ScrapRepository lost112ScrapRepository;
    private final ImageStorageService imageStorageService;

    @Value("${servers.match-server.url}")
    private String MATCH_SERVER_URL;

    public Long register(PostAcquiredBoardReqDto postAcquiredBoardReqDto) {
        List<String> imgKeys = postAcquiredBoardReqDto.getImgKeys();
        if (imgKeys == null || imgKeys.isEmpty()) {
            throw new IllegalArgumentException("이미지를 1개 이상 등록해야 합니다.");
        }
        // 형식·중복·스토리지 업로드 여부 확인, 이미 다른 게시글에 붙은 key는 거부
        imageStorageService.validateUploadedKeys(imgKeys);
        imgKeys.forEach(this::checkNotAttached);

        Member manager = memberQueryService.internalFindById(postAcquiredBoardReqDto.getMemberId());
        Board savedBoard = boardCommandRepository.save(Board.builder()
                .productName(postAcquiredBoardReqDto.getProductName())
                .member(manager)
                .thumbnailKey(imgKeys.get(0))
                .isLost(false)
                .status(BoardStatus.ONGOING)
                .build());

        List<ImgFile> imgFiles = new ArrayList<>();
        for (String imgKey : imgKeys) {
            ImgFile imgFile = new ImgFile(savedBoard, imgKey);
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
        // 작성자 본인 또는 같은 기관 관리자만 수정할 수 있다
        checkSameAgency(acquiredBoard.getBoard(), modifyReqDto.getMemberId());

        // imgKeys가 null이면 이미지는 그대로, 주어지면 게시글의 이미지가 정확히 그 목록(순서 포함)이 된다 (K-14)
        if (modifyReqDto.getImgKeys() != null) {
            if (modifyReqDto.getImgKeys().isEmpty()) {
                throw new IllegalArgumentException("이미지를 1개 이상 등록해야 합니다."); // 등록과 같은 규칙
            }
            imageStorageService.validateUploadedKeys(modifyReqDto.getImgKeys());
            modifyReqDto.setImgFileList(ImgFileSync.sync(acquiredBoard.getBoard(), modifyReqDto.getImgKeys(), imgFileRepository));
        }
        acquiredBoard.modify(modifyReqDto);

        return acquiredBoard.getBoard().getId();
    }

    private void checkNotAttached(String imgKey) {
        if (imgFileRepository.existsByImgKey(imgKey)) {
            throw new IllegalArgumentException("이미 다른 게시글에서 사용 중인 이미지입니다.");
        }
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

    /**
     * 작성자 본인이거나 작성자와 같은 기관 소속이면 통과, 아니면 403(AuthorizationServiceException).
     * 작성자에게 기관이 없으면(관리자에서 일반 회원으로 바뀐 경우) 작성자 본인만 통과한다.
     */
    private void checkSameAgency(Board board, Long memberId) {
        Member writer = board.getMember();
        if (writer.getId().equals(memberId)) {
            return;
        }
        Agency writersAgency = writer.getAgency();
        if (writersAgency != null) {
            Agency agency = memberQueryService.internalFindById(memberId).getAgency();
            if (agency != null && writersAgency.getId().equals(agency.getId())) {
                return;
            }
        }
        throw new AuthorizationServiceException("권한이 없습니다.");
    }
}
