package com.findear.main.board.command.service;

import com.findear.main.Alarm.service.NotificationService;
import com.findear.main.board.command.dto.GiveBackReqDto;
import com.findear.main.board.command.dto.ModifyAcquiredBoardReqDto;
import com.findear.main.board.command.dto.ModifyLostBoardReqDto;
import com.findear.main.board.command.repository.AcquiredBoardCommandRepository;
import com.findear.main.board.command.repository.BoardCommandRepository;
import com.findear.main.board.command.repository.ImgFileRepository;
import com.findear.main.board.command.repository.Lost112ScrapRepository;
import com.findear.main.board.command.repository.LostBoardCommandRepository;
import com.findear.main.board.command.repository.ReturnLogRepository;
import com.findear.main.board.command.repository.ScrapRepository;
import com.findear.main.board.common.domain.AcquiredBoard;
import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.board.query.repository.AcquiredBoardQueryRepository;
import com.findear.main.board.query.repository.BoardQueryRepository;
import com.findear.main.board.query.repository.LostBoardQueryRepository;
import com.findear.main.member.common.domain.Agency;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import com.findear.main.member.query.service.MemberQueryService;
import com.findear.main.storage.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AuthorizationServiceException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 게시글을 바꾸는 API의 권한: 분실물은 작성자 본인, 습득물은 작성자 본인 또는 같은 기관 (없으면 403). */
class BoardAuthorizationTest {

    private static final long WRITER = 1L;
    private static final long COLLEAGUE = 2L;   // 작성자와 같은 기관
    private static final long OUTSIDER = 3L;    // 다른 기관
    private static final long NORMAL = 4L;      // 기관 없음

    private MemberQueryService memberQueryService;
    private AcquiredBoardQueryRepository acquiredQueryRepository;
    private LostBoardQueryRepository lostQueryRepository;
    private BoardQueryRepository boardQueryRepository;
    private AcquiredBoardCommandServiceImpl acquiredService;
    private LostBoardCommandServiceImpl lostService;

    private final Agency agencyA = Agency.builder().id(10L).name("A").build();
    private final Agency agencyB = Agency.builder().id(20L).name("B").build();

    @BeforeEach
    void setUp() {
        memberQueryService = mock(MemberQueryService.class);
        when(memberQueryService.internalFindById(WRITER)).thenReturn(member(WRITER, agencyA));
        when(memberQueryService.internalFindById(COLLEAGUE)).thenReturn(member(COLLEAGUE, agencyA));
        when(memberQueryService.internalFindById(OUTSIDER)).thenReturn(member(OUTSIDER, agencyB));
        when(memberQueryService.internalFindById(NORMAL)).thenReturn(member(NORMAL, null));

        acquiredQueryRepository = mock(AcquiredBoardQueryRepository.class);
        lostQueryRepository = mock(LostBoardQueryRepository.class);
        boardQueryRepository = mock(BoardQueryRepository.class);
        acquiredService = new AcquiredBoardCommandServiceImpl(mock(AcquiredBoardCommandRepository.class), acquiredQueryRepository,
                mock(BoardCommandRepository.class), boardQueryRepository, memberQueryService, mock(ImgFileRepository.class),
                mock(ReturnLogRepository.class), mock(ScrapRepository.class), mock(Lost112ScrapRepository.class),
                mock(ImageStorageService.class));
        lostService = new LostBoardCommandServiceImpl(mock(LostBoardCommandRepository.class), memberQueryService,
                mock(ImgFileRepository.class), mock(BoardCommandRepository.class), boardQueryRepository, lostQueryRepository,
                mock(NotificationService.class), mock(ImageStorageService.class));
    }

    private static Member member(long id, Agency agency) {
        return Member.builder().id(id).naverUid("u" + id).phoneNumber("010" + id)
                .role(agency == null ? Role.NORMAL : Role.MANAGER).agency(agency).build();
    }

    private Board boardBy(Member writer) {
        return Board.builder().id(100L).isLost(false).member(writer).build();
    }

    private void stubAcquired(Board board) {
        AcquiredBoard acquired = AcquiredBoard.builder().id(5L).board(board).acquiredAt(LocalDate.now()).build();
        when(acquiredQueryRepository.findByBoardId(100L)).thenReturn(Optional.of(acquired));
    }

    private ModifyAcquiredBoardReqDto acquiredModifyBy(long memberId) {
        ModifyAcquiredBoardReqDto req = new ModifyAcquiredBoardReqDto();
        req.setBoardId(100L);
        req.setMemberId(memberId);
        return req;
    }

    private GiveBackReqDto giveBackBy(long managerId) {
        GiveBackReqDto req = new GiveBackReqDto();
        req.setBoardId(100L);
        req.setManagerId(managerId);
        req.setPhoneNumber("01000000000");
        return req;
    }

    // ---- 분실물 ----

    @Test
    @DisplayName("분실물 수정: 작성자만, 다른 회원은 403")
    void lostModifyOnlyWriter() {
        Board board = Board.builder().id(100L).isLost(true).member(member(WRITER, null)).build();
        LostBoard lost = LostBoard.builder().id(7L).board(board).lostAt(LocalDate.now()).build();
        when(lostQueryRepository.findByBoardId(100L)).thenReturn(Optional.of(lost));

        ModifyLostBoardReqDto other = new ModifyLostBoardReqDto();
        other.setBoardId(100L);
        other.setMemberId(OUTSIDER);
        assertThatThrownBy(() -> lostService.modify(other)).isInstanceOf(AuthorizationServiceException.class);

        ModifyLostBoardReqDto writer = new ModifyLostBoardReqDto();
        writer.setBoardId(100L);
        writer.setMemberId(WRITER);
        assertThat(lostService.modify(writer)).isEqualTo(100L);
    }

    @Test
    @DisplayName("분실물 삭제: 작성자만, 다른 회원은 403")
    void lostRemoveOnlyWriter() {
        Board board = Board.builder().id(100L).isLost(true).member(member(WRITER, null)).build();
        when(boardQueryRepository.findByIdAndDeleteYnFalse(100L)).thenReturn(Optional.of(board));

        assertThatThrownBy(() -> lostService.remove(100L, OUTSIDER)).isInstanceOf(AuthorizationServiceException.class);
        assertThatCode(() -> lostService.remove(100L, WRITER)).doesNotThrowAnyException();
    }

    // ---- 습득물 ----

    @Test
    @DisplayName("습득물 수정: 작성자와 같은 기관은 가능, 다른 기관·기관 없는 회원은 403")
    void acquiredModifyByAgency() {
        stubAcquired(boardBy(member(WRITER, agencyA)));

        assertThat(acquiredService.modify(acquiredModifyBy(WRITER))).isEqualTo(100L);
        assertThat(acquiredService.modify(acquiredModifyBy(COLLEAGUE))).isEqualTo(100L);
        assertThatThrownBy(() -> acquiredService.modify(acquiredModifyBy(OUTSIDER))).isInstanceOf(AuthorizationServiceException.class);
        assertThatThrownBy(() -> acquiredService.modify(acquiredModifyBy(NORMAL))).isInstanceOf(AuthorizationServiceException.class);
    }

    @Test
    @DisplayName("작성자에게 기관이 없으면(관리자에서 일반 회원으로 바뀜) 작성자 본인만 수정 가능하다")
    void acquiredWriterWithoutAgencyOnlyWriter() {
        stubAcquired(boardBy(member(NORMAL, null)));

        assertThat(acquiredService.modify(acquiredModifyBy(NORMAL))).isEqualTo(100L);
        assertThatThrownBy(() -> acquiredService.modify(acquiredModifyBy(OUTSIDER))).isInstanceOf(AuthorizationServiceException.class);
        assertThatThrownBy(() -> acquiredService.modify(acquiredModifyBy(COLLEAGUE))).isInstanceOf(AuthorizationServiceException.class);
    }

    @Test
    @DisplayName("습득물 삭제: 작성자만, 다른 회원은 403")
    void acquiredRemoveOnlyWriter() {
        Board board = boardBy(member(WRITER, agencyA));
        when(boardQueryRepository.findByIdAndDeleteYnFalse(100L)).thenReturn(Optional.of(board));

        assertThatThrownBy(() -> acquiredService.remove(100L, OUTSIDER)).isInstanceOf(AuthorizationServiceException.class);
        assertThatCode(() -> acquiredService.remove(100L, WRITER)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("인계·인계 취소: 같은 기관만, 다른 기관은 403 (IllegalArgumentException 400이 아님)")
    void giveBackAndRollbackSameAgencyOnly() {
        stubAcquired(boardBy(member(WRITER, agencyA)));

        assertThatThrownBy(() -> acquiredService.giveBack(giveBackBy(OUTSIDER))).isInstanceOf(AuthorizationServiceException.class);
        assertThatThrownBy(() -> acquiredService.giveBack(giveBackBy(NORMAL))).isInstanceOf(AuthorizationServiceException.class);
        assertThatThrownBy(() -> acquiredService.cancelGiveBack(OUTSIDER, 100L)).isInstanceOf(AuthorizationServiceException.class);
    }

    @Test
    @DisplayName("작성자에게 기관이 없으면 아무나 인계하던 구멍이 막혔다")
    void giveBackWhenWriterHasNoAgency() {
        stubAcquired(boardBy(member(NORMAL, null)));

        assertThatThrownBy(() -> acquiredService.giveBack(giveBackBy(OUTSIDER))).isInstanceOf(AuthorizationServiceException.class);
        assertThatThrownBy(() -> acquiredService.cancelGiveBack(COLLEAGUE, 100L)).isInstanceOf(AuthorizationServiceException.class);
    }
}
