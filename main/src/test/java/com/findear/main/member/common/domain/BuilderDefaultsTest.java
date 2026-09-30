package com.findear.main.member.common.domain;

import com.findear.main.board.common.domain.AcquiredBoard;
import com.findear.main.board.common.domain.Board;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

/** Lombok @Builder.Default: builder와 (JPA가 쓰는) no-args 생성자 양쪽에서 기본값이 유지되는지 확인. */
class BuilderDefaultsTest {

    @DisplayName("Member.builder()는 withdrawalYn=false, 컬렉션 필드는 빈 리스트")
    @Test
    void memberBuilderDefaults() {
        Member member = Member.builder().naverUid("uid").phoneNumber("010-0000-0000").role(Role.NORMAL).build();

        assertThat(member.getWithdrawalYn()).isFalse();
        assertThat(member.getMessageRoomList()).isNotNull().isEmpty();
        assertThat(member.getBoardList()).isNotNull().isEmpty();
        assertThat(member.getScrapList()).isNotNull().isEmpty();
        assertThat(member.getLost112ScrapList()).isNotNull().isEmpty();
        assertThat(member.getAlarmList()).isNotNull().isEmpty();
    }

    @DisplayName("Member no-args 생성자(JPA)도 같은 기본값을 유지한다")
    @Test
    void memberNoArgsDefaults() throws Exception {
        Constructor<Member> constructor = Member.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Member member = constructor.newInstance();

        assertThat(member.getWithdrawalYn()).isFalse();
        assertThat(member.getBoardList()).isNotNull().isEmpty();
        assertThat(member.getAlarmList()).isNotNull().isEmpty();
    }

    @DisplayName("Board.builder()는 deleteYn=false, 컬렉션은 빈 리스트")
    @Test
    void boardBuilderDefaults() {
        Board board = Board.builder().isLost(true).build();

        assertThat(board.getDeleteYn()).isFalse();
        assertThat(board.getMessageRoomList()).isNotNull().isEmpty();
        assertThat(board.getScrapList()).isNotNull().isEmpty();
        assertThat(board.getImgFileList()).isNotNull().isEmpty();
    }

    @DisplayName("Board/AcquiredBoard/Agency no-args 생성자(JPA)도 컬렉션 기본값을 유지한다")
    @Test
    void noArgsDefaults() throws Exception {
        Constructor<Board> boardCtor = Board.class.getDeclaredConstructor();
        boardCtor.setAccessible(true);
        Board board = boardCtor.newInstance();
        assertThat(board.getDeleteYn()).isFalse();
        assertThat(board.getImgFileList()).isNotNull().isEmpty();

        Constructor<AcquiredBoard> abCtor = AcquiredBoard.class.getDeclaredConstructor();
        abCtor.setAccessible(true);
        assertThat(abCtor.newInstance().getReturnLogList()).isNotNull().isEmpty();

        Constructor<Agency> agencyCtor = Agency.class.getDeclaredConstructor();
        agencyCtor.setAccessible(true);
        assertThat(agencyCtor.newInstance().getMemberList()).isNotNull().isEmpty();
    }

    @DisplayName("AcquiredBoard.builder()와 Agency.builder()는 컬렉션이 빈 리스트")
    @Test
    void otherBuilderDefaults() {
        assertThat(AcquiredBoard.builder().build().getReturnLogList()).isNotNull().isEmpty();
        assertThat(Agency.builder().build().getMemberList()).isNotNull().isEmpty();
    }
}
