package com.findear.main.matching;

import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.board.query.repository.AcquiredBoardQueryRepository;
import com.findear.main.board.query.repository.LostBoardQueryRepository;
import com.findear.main.matching.service.MatchingServiceImpl;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 매칭 결과(total)는 그 분실물 게시글의 작성자 본인만 본다. 타인(역할 무관)은 403, 없는 lostBoardId는 400이고
 * 둘 다 batch를 호출하기 전에 막는다. 호출 여부는 MockRestServiceServer로 확인한다 (기대 요청이 없으면 호출 즉시 실패).
 */
class MatchingOwnerAuthorizationTest {

    private static final long WRITER = 1L;
    private static final long MANAGER = 2L;
    private static final long LOST_BOARD_ID = 7L;
    private static final long MISSING_LOST_BOARD_ID = 999L;
    private static final String EMPTY_MATCHINGS =
            "{\"status\":200,\"message\":\"ok\",\"result\":{\"matchingList\":[],\"totalCount\":0}}";

    private MatchingServiceImpl service;
    private MockRestServiceServer batchServer;

    @BeforeEach
    void setUp() {
        RestTemplate batchRestTemplate = new RestTemplate();
        batchServer = MockRestServiceServer.bindTo(batchRestTemplate).build();

        Member writer = Member.builder().id(WRITER).naverUid("u1").phoneNumber("01000000001").role(Role.NORMAL).build();
        Board board = Board.builder().id(50L).isLost(true).member(writer).productName("지갑").build();
        LostBoardQueryRepository lostBoardQueryRepository = mock(LostBoardQueryRepository.class);
        when(lostBoardQueryRepository.findById(LOST_BOARD_ID))
                .thenReturn(Optional.of(LostBoard.builder().id(LOST_BOARD_ID).board(board).build()));
        when(lostBoardQueryRepository.findById(MISSING_LOST_BOARD_ID)).thenReturn(Optional.empty());

        service = new MatchingServiceImpl(batchRestTemplate, lostBoardQueryRepository, mock(AcquiredBoardQueryRepository.class));
    }

    private void expectBatchCall() {
        batchServer.expect(anything()).andRespond(withSuccess(EMPTY_MATCHINGS, MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("findear total: 작성자 본인은 검사를 통과해 batch를 호출하고 결과를 받는다")
    void findearOwnerPasses() {
        expectBatchCall();

        assertThat(service.getFindearMatchingList(WRITER, LOST_BOARD_ID, 1, 6).getMatchingList()).isEmpty();

        batchServer.verify();
    }

    @Test
    @DisplayName("lost112 total: 작성자 본인은 검사를 통과해 batch를 호출하고 결과를 받는다")
    void lost112OwnerPasses() {
        expectBatchCall();

        assertThat(service.getLost112MatchingList(WRITER, LOST_BOARD_ID, 1, 6).getMatchingList()).isEmpty();

        batchServer.verify();
    }

    @Test
    @DisplayName("findear total: 다른 회원(MANAGER 포함)은 403이고 batch를 부르지 않는다")
    void findearOtherMemberForbidden() {
        assertThatThrownBy(() -> service.getFindearMatchingList(MANAGER, LOST_BOARD_ID, 1, 6))
                .isInstanceOf(AuthorizationServiceException.class)
                .hasMessage("권한이 없습니다.");

        batchServer.verify();
    }

    @Test
    @DisplayName("lost112 total: 다른 회원(MANAGER 포함)은 403이고 batch를 부르지 않는다")
    void lost112OtherMemberForbidden() {
        assertThatThrownBy(() -> service.getLost112MatchingList(MANAGER, LOST_BOARD_ID, 1, 6))
                .isInstanceOf(AuthorizationServiceException.class)
                .hasMessage("권한이 없습니다.");

        batchServer.verify();
    }

    @Test
    @DisplayName("findear total: 없는 lostBoardId는 400(IllegalArgumentException)이고 batch를 부르지 않는다")
    void findearMissingLostBoard() {
        assertThatThrownBy(() -> service.getFindearMatchingList(WRITER, MISSING_LOST_BOARD_ID, 1, 6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("없는 게시물입니다.");

        batchServer.verify();
    }

    @Test
    @DisplayName("lost112 total: 없는 lostBoardId는 400(IllegalArgumentException)이고 batch를 부르지 않는다")
    void lost112MissingLostBoard() {
        assertThatThrownBy(() -> service.getLost112MatchingList(WRITER, MISSING_LOST_BOARD_ID, 1, 6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("없는 게시물입니다.");

        batchServer.verify();
    }
}
