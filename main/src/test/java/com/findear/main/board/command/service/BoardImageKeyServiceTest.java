package com.findear.main.board.command.service;

import com.findear.main.Alarm.service.NotificationService;
import com.findear.main.board.command.dto.ModifyAcquiredBoardReqDto;
import com.findear.main.board.command.dto.ModifyLostBoardReqDto;
import com.findear.main.board.command.dto.PostAcquiredBoardReqDto;
import com.findear.main.board.command.dto.PostLostBoardReqDto;
import com.findear.main.board.command.repository.AcquiredBoardCommandRepository;
import com.findear.main.board.command.repository.BoardCommandRepository;
import com.findear.main.board.command.repository.ImgFileRepository;
import com.findear.main.board.command.repository.Lost112ScrapRepository;
import com.findear.main.board.command.repository.LostBoardCommandRepository;
import com.findear.main.board.command.repository.ReturnLogRepository;
import com.findear.main.board.command.repository.ScrapRepository;
import com.findear.main.board.common.domain.AcquiredBoard;
import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.ImgFile;
import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.board.query.dto.AcquiredBoardDetailResDto;
import com.findear.main.board.query.repository.AcquiredBoardQueryRepository;
import com.findear.main.board.query.repository.BoardQueryRepository;
import com.findear.main.board.query.repository.LostBoardQueryRepository;
import com.findear.main.member.common.domain.Agency;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import com.findear.main.member.query.service.MemberQueryService;
import com.findear.main.storage.ImageStorageService;
import com.findear.main.storage.ImageUrls;
import com.findear.main.storage.StorageProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 게시글 등록·수정이 imgKeys를 검증하고 key만 저장하는지 (D-13). DB·네트워크 없이 Repository와 S3Client는 mock이고,
 * ImageStorageService는 실제 객체다 (HeadObject 응답만 mock).
 */
class BoardImageKeyServiceTest {

    private static final String KEY_1 = "images/2026/10/3f2b8c1e-1111-4222-8333-444455556666.jpg";
    private static final String KEY_2 = "images/2026/10/9a8b7c6d-1111-4222-8333-444455556666.png";
    private static final String KEY_3 = "images/2026/10/5e4d3c2b-1111-4222-8333-444455556666.webp";
    private static final String MISSING = "images/2026/10/00000000-1111-4222-8333-444455556666.jpg";

    private S3Client s3Client;
    private ImageStorageService imageStorageService;
    private ImgFileRepository imgFileRepository;
    private BoardCommandRepository boardCommandRepository;
    private MemberQueryService memberQueryService;
    private AcquiredBoardCommandRepository acquiredBoardCommandRepository;
    private AcquiredBoardQueryRepository acquiredBoardQueryRepository;
    private LostBoardCommandRepository lostBoardCommandRepository;
    private LostBoardQueryRepository lostBoardQueryRepository;
    private AcquiredBoardCommandServiceImpl acquiredService;
    private LostBoardCommandServiceImpl lostService;

    @BeforeEach
    void setUp() {
        ImageUrls.configure("http://localhost:8333/findear-images");
        s3Client = mock(S3Client.class);
        // MISSING만 스토리지에 없다
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenAnswer(invocation -> {
            HeadObjectRequest request = invocation.getArgument(0);
            if (MISSING.equals(request.key())) {
                throw NoSuchKeyException.builder().build();
            }
            return HeadObjectResponse.builder().build();
        });
        imageStorageService = new ImageStorageService(s3Client, mock(S3Presigner.class), new StorageProperties());

        imgFileRepository = mock(ImgFileRepository.class);
        when(imgFileRepository.save(any(ImgFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        boardCommandRepository = mock(BoardCommandRepository.class);
        when(boardCommandRepository.save(any(Board.class))).thenAnswer(invocation -> invocation.getArgument(0));
        acquiredBoardCommandRepository = mock(AcquiredBoardCommandRepository.class);
        when(acquiredBoardCommandRepository.save(any(AcquiredBoard.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lostBoardCommandRepository = mock(LostBoardCommandRepository.class);
        when(lostBoardCommandRepository.save(any(LostBoard.class))).thenAnswer(invocation -> invocation.getArgument(0));
        acquiredBoardQueryRepository = mock(AcquiredBoardQueryRepository.class);
        lostBoardQueryRepository = mock(LostBoardQueryRepository.class);

        memberQueryService = mock(MemberQueryService.class);
        Agency agency = Agency.builder().id(1L).name("서울역 유실물센터").address("서울 용산구").xPos(126.97f).yPos(37.55f).build();
        Member manager = Member.builder().id(2L).naverUid("uid").phoneNumber("010-0000-0002").role(Role.MANAGER).agency(agency).build();
        when(memberQueryService.internalFindById(2L)).thenReturn(manager);
        Member normal = Member.builder().id(1L).naverUid("uid1").phoneNumber("010-0000-0001").role(Role.NORMAL).build();
        when(memberQueryService.internalFindById(1L)).thenReturn(normal);

        acquiredService = new AcquiredBoardCommandServiceImpl(acquiredBoardCommandRepository, acquiredBoardQueryRepository,
                boardCommandRepository, mock(BoardQueryRepository.class), memberQueryService, imgFileRepository,
                mock(ReturnLogRepository.class), mock(ScrapRepository.class), mock(Lost112ScrapRepository.class),
                imageStorageService);
        lostService = new LostBoardCommandServiceImpl(lostBoardCommandRepository, memberQueryService, imgFileRepository,
                boardCommandRepository, mock(BoardQueryRepository.class), lostBoardQueryRepository,
                mock(NotificationService.class), imageStorageService);
        // 등록 뒤의 match·batch 비동기 호출은 루프백의 닫힌 포트로 보내 즉시 실패시킨다 (외부로 나가는 요청 없음, 실패는 서비스가 로그만 남김)
        ReflectionTestUtils.setField(acquiredService, "MATCH_SERVER_URL", "http://127.0.0.1:1");
        ReflectionTestUtils.setField(lostService, "BATCH_SERVER_URL", "http://127.0.0.1:1");
    }

    @AfterEach
    void tearDown() {
        ImageUrls.configure(ImageUrls.DEFAULT_BASE_URL);
    }

    // ---- 습득물 등록 ----

    @DisplayName("습득물 등록: 정상 key는 ImgFile.imgKey와 Board.thumbnailKey(첫 key)로 저장된다")
    @Test
    void acquiredRegisterStoresKeys() {
        PostAcquiredBoardReqDto req = new PostAcquiredBoardReqDto();
        req.setMemberId(2L);
        req.setProductName("검은색 지갑");
        req.setImgKeys(List.of(KEY_1, KEY_2));

        acquiredService.register(req);

        ArgumentCaptor<Board> board = ArgumentCaptor.forClass(Board.class);
        verify(boardCommandRepository).save(board.capture());
        assertThat(board.getValue().getThumbnailKey()).isEqualTo(KEY_1);
        ArgumentCaptor<ImgFile> imgFiles = ArgumentCaptor.forClass(ImgFile.class);
        verify(imgFileRepository, org.mockito.Mockito.times(2)).save(imgFiles.capture());
        assertThat(imgFiles.getAllValues()).extracting(ImgFile::getImgKey).containsExactly(KEY_1, KEY_2);
    }

    @DisplayName("습득물 등록: 스토리지에 없는 key면 예외, 아무것도 저장하지 않는다")
    @Test
    void acquiredRegisterRejectsMissingKey() {
        PostAcquiredBoardReqDto req = new PostAcquiredBoardReqDto();
        req.setMemberId(2L);
        req.setProductName("검은색 지갑");
        req.setImgKeys(List.of(KEY_1, MISSING));

        assertThatThrownBy(() -> acquiredService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("업로드되지 않은");
        verify(boardCommandRepository, never()).save(any(Board.class));
        verify(imgFileRepository, never()).save(any(ImgFile.class));
    }

    @DisplayName("습득물 등록: 이미지가 없으면 거부, URL 형태나 다른 경로 key도 거부")
    @Test
    void acquiredRegisterRejectsEmptyAndMalformed() {
        List<List<String>> cases = java.util.Arrays.asList(List.of(), null, List.of("https://evil.example.com/a.jpg"), List.of("uploads/a.jpg"));
        for (List<String> keys : cases) {
            PostAcquiredBoardReqDto req = new PostAcquiredBoardReqDto();
            req.setMemberId(2L);
            req.setImgKeys(keys);
            assertThatThrownBy(() -> acquiredService.register(req)).isInstanceOf(IllegalArgumentException.class);
        }
        verify(boardCommandRepository, never()).save(any(Board.class));
    }

    @DisplayName("습득물 등록: 이미 다른 게시글에 붙은 key는 거부")
    @Test
    void acquiredRegisterRejectsKeyInUse() {
        when(imgFileRepository.existsByImgKey(KEY_1)).thenReturn(true);
        PostAcquiredBoardReqDto req = new PostAcquiredBoardReqDto();
        req.setMemberId(2L);
        req.setImgKeys(List.of(KEY_1));

        assertThatThrownBy(() -> acquiredService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미");
        verify(boardCommandRepository, never()).save(any(Board.class));
    }

    // ---- 분실물 등록 ----

    @DisplayName("분실물 등록: 정상 key는 ImgFile.imgKey와 Board.thumbnailKey(첫 key)로 저장된다")
    @Test
    void lostRegisterStoresKeys() {
        PostLostBoardReqDto req = lostRequest(List.of(KEY_2, KEY_1));

        lostService.register(req);

        ArgumentCaptor<Board> board = ArgumentCaptor.forClass(Board.class);
        verify(boardCommandRepository).save(board.capture());
        assertThat(board.getValue().getThumbnailKey()).isEqualTo(KEY_2);
        ArgumentCaptor<ImgFile> imgFiles = ArgumentCaptor.forClass(ImgFile.class);
        verify(imgFileRepository, org.mockito.Mockito.times(2)).save(imgFiles.capture());
        assertThat(imgFiles.getAllValues()).extracting(ImgFile::getImgKey).containsExactly(KEY_2, KEY_1);
        // 분실물 엔티티로 넘어가는 board에도 thumbnailKey가 유지된다 (BoardDto -> Board)
        ArgumentCaptor<LostBoard> lost = ArgumentCaptor.forClass(LostBoard.class);
        verify(lostBoardCommandRepository).save(lost.capture());
        assertThat(lost.getValue().getBoard().getThumbnailKey()).isEqualTo(KEY_2);
    }

    @DisplayName("분실물 등록: 이미지 없이도 등록되고 thumbnailKey는 null")
    @Test
    void lostRegisterWithoutImages() {
        lostService.register(lostRequest(null));

        ArgumentCaptor<Board> board = ArgumentCaptor.forClass(Board.class);
        verify(boardCommandRepository).save(board.capture());
        assertThat(board.getValue().getThumbnailKey()).isNull();
        verify(imgFileRepository, never()).save(any(ImgFile.class));
    }

    @DisplayName("분실물 등록: 스토리지에 없는 key면 예외")
    @Test
    void lostRegisterRejectsMissingKey() {
        assertThatThrownBy(() -> lostService.register(lostRequest(List.of(MISSING))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("업로드되지 않은");
        verify(boardCommandRepository, never()).save(any(Board.class));
    }

    // ---- 수정 (K-14: 이미지는 요청한 imgKeys와 정확히 같아진다) ----

    private Board boardWith(long id, String... keys) {
        List<ImgFile> files = new ArrayList<>();
        long fileId = id * 100;
        for (String key : keys) {
            files.add(new ImgFile(++fileId, key));
        }
        return Board.builder().id(id).isLost(false).thumbnailKey(keys.length == 0 ? null : keys[0])
                .imgFileList(files).build();
    }

    private AcquiredBoard acquiredOf(Board board) {
        AcquiredBoard acquired = AcquiredBoard.builder().id(5L).board(board).acquiredAt(LocalDate.now()).build();
        when(acquiredBoardQueryRepository.findByBoardId(board.getId())).thenReturn(Optional.of(acquired));
        return acquired;
    }

    private LostBoard lostOf(Board board) {
        LostBoard lost = LostBoard.builder().id(7L).board(board).lostAt(LocalDate.now()).build();
        when(lostBoardQueryRepository.findByBoardId(board.getId())).thenReturn(Optional.of(lost));
        return lost;
    }

    private static ModifyAcquiredBoardReqDto acquiredModify(long boardId, List<String> keys) {
        ModifyAcquiredBoardReqDto req = new ModifyAcquiredBoardReqDto();
        req.setBoardId(boardId);
        req.setImgKeys(keys);
        return req;
    }

    private static ModifyLostBoardReqDto lostModify(long boardId, List<String> keys) {
        ModifyLostBoardReqDto req = new ModifyLostBoardReqDto();
        req.setBoardId(boardId);
        req.setImgKeys(keys);
        return req;
    }

    /** 이 게시글에 이미 붙은 key는 리포지토리가 그 행을 돌려준다 (findFirstByImgKey) */
    private void stubExisting(Board board) {
        for (ImgFile file : board.getImgFileList()) {
            when(imgFileRepository.findFirstByImgKey(file.getImgKey())).thenReturn(Optional.of(file));
        }
    }

    @SuppressWarnings("unchecked")
    private List<ImgFile> deleted() {
        ArgumentCaptor<List<ImgFile>> captor = ArgumentCaptor.forClass(List.class);
        verify(imgFileRepository).deleteAll(captor.capture());
        return captor.getValue();
    }

    @DisplayName("습득물 수정: 전부 교체하면 옛 행은 삭제되고 새 key만 남으며 썸네일은 새 첫 key")
    @Test
    void acquiredModifyReplacesAll() {
        Board board = boardWith(10L, KEY_1, KEY_2);
        List<ImgFile> old = List.copyOf(board.getImgFileList());
        stubExisting(board);
        acquiredOf(board);

        acquiredService.modify(acquiredModify(10L, List.of(KEY_3)));

        assertThat(deleted()).containsExactlyElementsOf(old);
        assertThat(board.getImgFileList()).extracting(ImgFile::getImgKey).containsExactly(KEY_3);
        assertThat(board.getThumbnailKey()).isEqualTo(KEY_3);
    }

    @DisplayName("습득물 수정: 끝에 key를 추가하면 기존 행은 유지되고 새 행만 저장된다")
    @Test
    void acquiredModifyKeepsExistingAndAppends() {
        Board board = boardWith(10L, KEY_1, KEY_2);
        List<ImgFile> old = List.copyOf(board.getImgFileList());
        stubExisting(board);
        acquiredOf(board);

        acquiredService.modify(acquiredModify(10L, List.of(KEY_1, KEY_2, KEY_3)));

        verify(imgFileRepository, never()).deleteAll(any());
        ArgumentCaptor<ImgFile> saved = ArgumentCaptor.forClass(ImgFile.class);
        verify(imgFileRepository).save(saved.capture());
        assertThat(saved.getValue().getImgKey()).isEqualTo(KEY_3);
        assertThat(board.getImgFileList()).hasSize(3);
        assertThat(board.getImgFileList().get(0)).isSameAs(old.get(0));
        assertThat(board.getImgFileList().get(1)).isSameAs(old.get(1));
        assertThat(board.getThumbnailKey()).isEqualTo(KEY_1);
    }

    @DisplayName("습득물 수정: 끝의 key를 빼면 그 행만 삭제된다")
    @Test
    void acquiredModifyRemovesTail() {
        Board board = boardWith(10L, KEY_1, KEY_2);
        List<ImgFile> old = List.copyOf(board.getImgFileList());
        stubExisting(board);
        acquiredOf(board);

        acquiredService.modify(acquiredModify(10L, List.of(KEY_1)));

        assertThat(deleted()).containsExactly(old.get(1));
        verify(imgFileRepository, never()).save(any(ImgFile.class));
        assertThat(board.getImgFileList()).containsExactly(old.get(0));
    }

    @DisplayName("습득물 수정: 순서를 바꾸면 바뀐 지점부터 다시 만들어 요청 순서(=id 순서)가 된다")
    @Test
    void acquiredModifyReorders() {
        Board board = boardWith(10L, KEY_1, KEY_2);
        List<ImgFile> old = List.copyOf(board.getImgFileList());
        stubExisting(board);
        acquiredOf(board);

        acquiredService.modify(acquiredModify(10L, List.of(KEY_2, KEY_1)));

        assertThat(deleted()).containsExactlyElementsOf(old);
        ArgumentCaptor<ImgFile> saved = ArgumentCaptor.forClass(ImgFile.class);
        verify(imgFileRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(ImgFile::getImgKey).containsExactly(KEY_2, KEY_1);
        assertThat(board.getImgFileList()).extracting(ImgFile::getImgKey).containsExactly(KEY_2, KEY_1);
        assertThat(board.getThumbnailKey()).isEqualTo(KEY_2);
    }

    @DisplayName("습득물 수정: 다른 게시글에 붙은 key는 거부하고 아무것도 삭제·저장하지 않는다")
    @Test
    void acquiredModifyRejectsOtherBoardsKey() {
        Board board = boardWith(10L, KEY_1);
        stubExisting(board);
        Board other = boardWith(99L, KEY_2);
        stubExisting(other);
        acquiredOf(board);

        assertThatThrownBy(() -> acquiredService.modify(acquiredModify(10L, List.of(KEY_2))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미");

        verify(imgFileRepository, never()).deleteAll(any());
        verify(imgFileRepository, never()).save(any(ImgFile.class));
        assertThat(board.getImgFileList()).extracting(ImgFile::getImgKey).containsExactly(KEY_1);
        assertThat(board.getThumbnailKey()).isEqualTo(KEY_1);
    }

    @DisplayName("습득물 수정: imgKeys가 null이면 이미지는 그대로")
    @Test
    void acquiredModifyNullKeepsImages() {
        Board board = boardWith(10L, KEY_1, KEY_2);
        acquiredOf(board);
        ModifyAcquiredBoardReqDto req = acquiredModify(10L, null);
        req.setColor("red");

        acquiredService.modify(req);

        verify(imgFileRepository, never()).deleteAll(any());
        verify(imgFileRepository, never()).save(any(ImgFile.class));
        assertThat(board.getImgFileList()).extracting(ImgFile::getImgKey).containsExactly(KEY_1, KEY_2);
        assertThat(board.getThumbnailKey()).isEqualTo(KEY_1);
        assertThat(board.getColor()).isEqualTo("red");
    }

    @DisplayName("습득물 수정: 빈 목록은 등록과 같은 규칙으로 거부 (이미지 1개 이상)")
    @Test
    void acquiredModifyEmptyRejected() {
        Board board = boardWith(10L, KEY_1);
        acquiredOf(board);

        assertThatThrownBy(() -> acquiredService.modify(acquiredModify(10L, List.of())))
                .isInstanceOf(IllegalArgumentException.class);

        verify(imgFileRepository, never()).deleteAll(any());
        assertThat(board.getImgFileList()).hasSize(1);
    }

    @DisplayName("습득물 수정: 스토리지에 없는 key는 거부하고 기존 이미지는 그대로")
    @Test
    void acquiredModifyMissingKeyRejected() {
        Board board = boardWith(10L, KEY_1);
        acquiredOf(board);

        assertThatThrownBy(() -> acquiredService.modify(acquiredModify(10L, List.of(MISSING))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("업로드되지 않은");
        verify(imgFileRepository, never()).deleteAll(any());
        assertThat(board.getImgFileList()).hasSize(1);
    }

    @DisplayName("분실물 수정: 교체하면 옛 행 삭제, 새 key 저장, 썸네일은 새 첫 key")
    @Test
    void lostModifyReplaces() {
        Board board = boardWith(20L, KEY_1, KEY_2);
        List<ImgFile> old = List.copyOf(board.getImgFileList());
        stubExisting(board);
        lostOf(board);

        lostService.modify(lostModify(20L, List.of(KEY_2, KEY_3)));

        // KEY_1이 첫 자리에서 빠지므로 첫 행부터 다시 만든다
        assertThat(deleted()).containsExactlyElementsOf(old);
        assertThat(board.getImgFileList()).extracting(ImgFile::getImgKey).containsExactly(KEY_2, KEY_3);
        assertThat(board.getThumbnailKey()).isEqualTo(KEY_2);
    }

    @DisplayName("분실물 수정: 빈 목록이면 이미지를 모두 제거하고 thumbnailKey는 null")
    @Test
    void lostModifyEmptyRemovesAll() {
        Board board = boardWith(20L, KEY_1, KEY_2);
        List<ImgFile> old = List.copyOf(board.getImgFileList());
        stubExisting(board);
        lostOf(board);

        lostService.modify(lostModify(20L, List.of()));

        assertThat(deleted()).containsExactlyElementsOf(old);
        verify(imgFileRepository, never()).save(any(ImgFile.class));
        assertThat(board.getImgFileList()).isEmpty();
        assertThat(board.getThumbnailKey()).isNull();
    }

    @DisplayName("분실물 수정: imgKeys가 null이면 이미지는 그대로, 다른 게시글의 key는 거부")
    @Test
    void lostModifyNullAndOwnership() {
        Board board = boardWith(20L, KEY_1);
        stubExisting(board);
        Board other = boardWith(99L, KEY_2);
        stubExisting(other);
        lostOf(board);

        lostService.modify(lostModify(20L, null));
        verify(imgFileRepository, never()).deleteAll(any());
        assertThat(board.getImgFileList()).extracting(ImgFile::getImgKey).containsExactly(KEY_1);
        assertThat(board.getThumbnailKey()).isEqualTo(KEY_1);

        assertThatThrownBy(() -> lostService.modify(lostModify(20L, List.of(KEY_2))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(imgFileRepository, never()).deleteAll(any());
        assertThat(board.getImgFileList()).extracting(ImgFile::getImgKey).containsExactly(KEY_1);
    }

    // ---- 응답 ----

    @DisplayName("습득물 상세 응답의 imgUrls는 key가 아니라 공개 URL")
    @Test
    void detailResponseAssemblesUrls() {
        Member writer = Member.builder().id(2L).phoneNumber("010-0000-0002").role(Role.MANAGER).build();
        List<ImgFile> files = new ArrayList<>(List.of(new ImgFile(1L, KEY_1), new ImgFile(2L, KEY_2)));
        Board board = Board.builder().id(10L).isLost(false).member(writer).imgFileList(files)
                .thumbnailKey(KEY_1).registeredAt(LocalDateTime.now()).build();
        AcquiredBoard acquired = AcquiredBoard.builder().id(5L).board(board).acquiredAt(LocalDate.now()).build();

        AcquiredBoardDetailResDto res = AcquiredBoardDetailResDto.of(acquired);

        assertThat(res.getBoard().getImgUrls()).containsExactly(
                "http://localhost:8333/findear-images/" + KEY_1,
                "http://localhost:8333/findear-images/" + KEY_2);
    }

    private static PostLostBoardReqDto lostRequest(List<String> keys) {
        PostLostBoardReqDto req = new PostLostBoardReqDto();
        req.setMemberId(1L);
        req.setProductName("지갑");
        req.setCategory("지갑");
        req.setColor("검정");
        req.setContent("검정 가죽 지갑");
        req.setImgKeys(keys);
        req.setLostAt("2026-09-29");
        req.setXpos("126.97");
        req.setYpos("37.55");
        req.setSuspiciousPlace("서울역");
        return req;
    }
}
