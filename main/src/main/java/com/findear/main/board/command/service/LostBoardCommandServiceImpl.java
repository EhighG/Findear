package com.findear.main.board.command.service;

import com.findear.main.Alarm.dto.NotificationRequestDto;
import com.findear.main.Alarm.service.NotificationService;
import com.findear.main.board.command.dto.MatchingFindearDatasReqDto;
import com.findear.main.board.command.dto.ModifyLostBoardReqDto;
import com.findear.main.board.command.dto.PostLostBoardReqDto;
import com.findear.main.board.command.repository.BoardCommandRepository;
import com.findear.main.board.command.repository.ImgFileRepository;
import com.findear.main.board.command.repository.LostBoardCommandRepository;
import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.BoardStatus;
import com.findear.main.board.common.domain.ImgFile;
import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.board.common.dto.BoardDto;
import com.findear.main.board.common.dto.LostBoardDto;
import com.findear.main.board.query.dto.BatchServerResponseDto;
import com.findear.main.board.query.repository.BoardQueryRepository;
import com.findear.main.board.query.repository.LostBoardQueryRepository;
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
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class LostBoardCommandServiceImpl implements LostBoardCommandService {

    private final LostBoardCommandRepository lostBoardCommandRepository;
    private final MemberQueryService memberQueryService;
    private final ImgFileRepository imgFileRepository;
    private final BoardCommandRepository boardCommandRepository;
    private final BoardQueryRepository boardQueryRepository;
    private final LostBoardQueryRepository lostBoardQueryRepository;
    private final NotificationService notificationService;
    private final ImageStorageService imageStorageService;

    @Value("${servers.batch-server.url}")
    private String BATCH_SERVER_URL;

    public Long register(PostLostBoardReqDto postLostBoardReqDto) {

        log.info("들어온 데이터 : " + postLostBoardReqDto.toString());
        List<String> imgKeys = postLostBoardReqDto.getImgKeys() == null ? List.of() : postLostBoardReqDto.getImgKeys();
        // 형식·중복·스토리지 업로드 여부 확인, 이미 다른 게시글에 붙은 key는 거부
        imageStorageService.validateUploadedKeys(imgKeys);
        imgKeys.forEach(this::checkNotAttached);
        Member member = memberQueryService.internalFindById(postLostBoardReqDto.getMemberId());
        Board savedBoard = boardCommandRepository.save(Board.builder()
                .productName(postLostBoardReqDto.getProductName())
                .member(member)
                .color(postLostBoardReqDto.getColor())
                .aiDescription(postLostBoardReqDto.getContent())
                .thumbnailKey(imgKeys.isEmpty() ? null : imgKeys.get(0))
                .categoryName(postLostBoardReqDto.getCategory())
                .isLost(true)
                .status(BoardStatus.ONGOING)
                .build()
        );

        log.info("이미지 등록");
        // 이미지 등록
        List<ImgFile> imgFiles = new ArrayList<>();
        for (String imgKey : imgKeys) {
            ImgFile imgFile = new ImgFile(savedBoard, imgKey);
            ImgFile savedFile = imgFileRepository.save(imgFile);
            imgFiles.add(savedFile);
        }
        savedBoard.updateImgFiles(imgFiles);

        LostBoardDto lostBoardDto = LostBoardDto.builder()
                .board(BoardDto.of(savedBoard))
                .lostAt(postLostBoardReqDto.getLostAt())
                .suspiciousPlace(postLostBoardReqDto.getSuspiciousPlace())
                .xPos(Float.parseFloat(postLostBoardReqDto.getXpos()))
                .yPos(Float.parseFloat(postLostBoardReqDto.getYpos()))
                .build();

        LostBoard saveResult = lostBoardCommandRepository.save(lostBoardDto.toEntity());

        requestFirstMatching(saveResult)
                .subscribe(
                        this::parseAndRequestAlert,
                        error -> {
                            log.error("분실물 등록 후 첫 매칭 요청 실패 \nerror = " + error);
                            error.printStackTrace();
                        }
                );

        return savedBoard.getId();
    }

    public Long modify(ModifyLostBoardReqDto modifyReqDto) {
        LostBoard lostBoard = lostBoardQueryRepository.findByBoardId(modifyReqDto.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다."));
        // 작성자 본인만 수정할 수 있다
        if (!lostBoard.getBoard().getMember().getId().equals(modifyReqDto.getMemberId())) {
            throw new AuthorizationServiceException("권한이 없습니다.");
        }
        // imgKeys가 null이면 이미지는 그대로, 주어지면 게시글의 이미지가 정확히 그 목록(순서 포함)이 된다. 빈 목록이면 이미지를 모두 제거한다 (K-14)
        if (modifyReqDto.getImgKeys() != null) {
            imageStorageService.validateUploadedKeys(modifyReqDto.getImgKeys());
            modifyReqDto.setImgFileList(ImgFileSync.sync(lostBoard.getBoard(), modifyReqDto.getImgKeys(), imgFileRepository));
        }
        lostBoard.modify(modifyReqDto);

        return lostBoard.getBoard().getId();
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


    private Mono<BatchServerResponseDto> requestFirstMatching(LostBoard saveResult) {
        MatchingFindearDatasReqDto matchingFindearDatasReqDto = MatchingFindearDatasReqDto.builder()
                .lostBoardId(saveResult.getId())
                .productName(saveResult.getBoard().getProductName())
                .color(saveResult.getBoard().getColor())
                .categoryName(saveResult.getBoard().getCategoryName())
                .description(saveResult.getBoard().getAiDescription())
                .lostAt(saveResult.getLostAt().toString())
                .xPos(saveResult.getXPos())
                .yPos(saveResult.getYPos())
                .build();

        log.info("batch 서버로 요청 로직");
        // batch 서버로 요청
        String serverURL = BATCH_SERVER_URL + "/findear/matching";

        WebClient client = WebClient.builder()
                .baseUrl(serverURL)
                .codecs (it -> it.defaultCodecs().maxInMemorySize(1024 * 1024 * 10) // 1MB 제한
                )
                .build();

        return client.post()
                .bodyValue(matchingFindearDatasReqDto)
                .retrieve()
                .bodyToMono(BatchServerResponseDto.class);
    }

    private void parseAndRequestAlert(BatchServerResponseDto batchServerResponseDto) {
        try {
            LinkedHashMap<String, Object> resultMap = (LinkedHashMap<String, Object>) batchServerResponseDto.getResult();
            if (resultMap == null) return;
            List<LinkedHashMap<String, Object>> result = (List<LinkedHashMap<String, Object>>)resultMap.get("findearDatas");

            Long lostBoardId = ((Integer) result.get(0).get("lostBoardId")).longValue();

            Long losterId = lostBoardQueryRepository.findById(lostBoardId).get()
                    .getBoard().getMember().getId();
            // 알림 메소드 호출
            notificationService.sendNotification(NotificationRequestDto.builder()
                    .title("등록하신 분실물과 유사한 물건들을 찾아봤어요!")
                    .message("매칭이 완료되었습니다.")
                    .type("message")
                    .memberId(losterId)
                    .build());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("분실물 등록 후 첫 매칭 결과 파싱 중 오류");
        }
    }
}
