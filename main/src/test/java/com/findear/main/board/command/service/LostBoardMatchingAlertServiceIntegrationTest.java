package com.findear.main.board.command.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findear.main.Alarm.common.domain.Alarm;
import com.findear.main.Alarm.repository.AlarmRepository;
import com.findear.main.board.command.repository.BoardCommandRepository;
import com.findear.main.board.command.repository.LostBoardCommandRepository;
import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.BoardStatus;
import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.board.query.dto.BatchServerResponseDto;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import com.findear.main.support.IntegrationTest;
import com.findear.main.support.TestMembers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 매칭 알림: 실제 MySQL(Testcontainers)에서 분실물을 id로 다시 읽어 작성자에게 tbl_alarm 1건을 남기는지 확인한다 (FCM 비활성). */
@IntegrationTest
@Transactional
class LostBoardMatchingAlertServiceIntegrationTest {

    private static final String MATCHED = "{\"status\":200,\"message\":\"습득물 매칭 성공\",\"result\":{"
            + "\"findearDatas\":[{\"lostBoardId\":1,\"acquiredBoardId\":3,\"similarityRate\":0.9}],\"policeDatas\":[]}}";
    private static final String EMPTY = "{\"status\":200,\"message\":\"습득물 매칭 성공\",\"result\":{"
            + "\"findearDatas\":[],\"policeDatas\":[]}}";

    @Autowired LostBoardMatchingAlertService alertService;
    @Autowired TestMembers testMembers;
    @Autowired BoardCommandRepository boardCommandRepository;
    @Autowired LostBoardCommandRepository lostBoardCommandRepository;
    @Autowired AlarmRepository alarmRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private LostBoard saveLostBoard(Member writer, boolean deleted) {
        Board board = boardCommandRepository.save(Board.builder().isLost(true).productName("검정 가죽 지갑")
                .categoryName("지갑").member(writer).deleteYn(deleted).status(BoardStatus.ONGOING).build());
        return lostBoardCommandRepository.save(LostBoard.builder().board(board).lostAt(LocalDate.now().minusDays(1))
                .xPos(126.97f).yPos(37.55f).build());
    }

    private BatchServerResponseDto response(String json) throws Exception {
        return objectMapper.readValue(json, BatchServerResponseDto.class);
    }

    @DisplayName("매칭 결과가 있으면 작성자에게 알림이 1건 저장된다")
    @Test
    void savesAlarmForWriter() throws Exception {
        Member writer = testMembers.newMember(Role.NORMAL);
        Member other = testMembers.newMember(Role.NORMAL);
        LostBoard lostBoard = saveLostBoard(writer, false);

        alertService.alertIfMatched(lostBoard.getId(), response(MATCHED));

        List<Alarm> alarms = alarmRepository.findAllByMemberId(writer.getId());
        assertThat(alarms).hasSize(1);
        assertThat(alarms.get(0).getContent()).isEqualTo("매칭이 완료되었습니다.");
        assertThat(alarmRepository.findAllByMemberId(other.getId())).isEmpty();
    }

    @DisplayName("삭제된 분실물이면 알림이 없다")
    @Test
    void skipsDeletedLostBoard() throws Exception {
        Member writer = testMembers.newMember(Role.NORMAL);
        LostBoard lostBoard = saveLostBoard(writer, true);

        alertService.alertIfMatched(lostBoard.getId(), response(MATCHED));

        assertThat(alarmRepository.findAllByMemberId(writer.getId())).isEmpty();
    }

    @DisplayName("없는 분실물 id면 알림 없이 예외도 없다")
    @Test
    void skipsMissingLostBoard() throws Exception {
        alertService.alertIfMatched(999_999_999L, response(MATCHED));
    }

    @DisplayName("둘 다 빈 결과면 알림이 없다")
    @Test
    void noAlarmWhenNoMatches() throws Exception {
        Member writer = testMembers.newMember(Role.NORMAL);
        LostBoard lostBoard = saveLostBoard(writer, false);

        alertService.alertIfMatched(lostBoard.getId(), response(EMPTY));

        assertThat(alarmRepository.findAllByMemberId(writer.getId())).isEmpty();
    }
}
