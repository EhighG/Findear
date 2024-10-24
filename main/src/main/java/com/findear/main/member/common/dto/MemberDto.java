package com.findear.main.member.common.dto;

import com.findear.main.Alarm.common.domain.Alarm;
import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.Scrap;
import com.findear.main.member.common.domain.Agency;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import com.findear.main.message.common.domain.MessageRoom;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDto {
    private Long id;
    private String naverUid;

////    private List<MessageRoom> messageRoomList = new ArrayList<>();
//    private List<Long> messageRoom
//
//
//    private List<Board> boardList = new ArrayList<>();
//
//    private List<Scrap> scrapList = new ArrayList<>();
//
//    private List<Alarm> alarmList = new ArrayList<>();

    private Agency agency;

    private Role role;

    private String phoneNumber;

    private LocalDateTime joinedAt;

    private LocalDateTime withdrawalAt;

    private Boolean withdrawalYn;
    private String naverRefreshToken;

    public static MemberDto of(Member member) {
        return MemberDto.builder()
                .id(member.getId())
                .naverUid(member.getNaverUid())
//                .messageRoomList(member.getMessageRoomList())
                .agency(member.getAgency())
//                .scrapList(member.getScrapList())
//                .alarmList(member.getAlarmList())
//                .boardList(member.getBoardList())
                .phoneNumber(member.getPhoneNumber())
                .joinedAt(member.getJoinedAt())
                .withdrawalAt(member.getWithdrawalAt())
                .withdrawalYn(member.getWithdrawalYn())
                .role(member.getRole())
                .naverRefreshToken(member.getNaverRefreshToken())
                .build();
    }

    public Member toEntity() {
        return Member.builder()
                .id(id)
                .naverUid(naverUid)
//                .messageRoomList(messageRoomList)
                .agency(agency)
//                .scrapList(scrapList)
//                .alarmList(alarmList)
//                .boardList(boardList)
                .phoneNumber(phoneNumber)
                .joinedAt(joinedAt)
                .withdrawalAt(withdrawalAt)
                .withdrawalYn(withdrawalYn)
                .role(role)
                .naverRefreshToken(naverRefreshToken)
                .build();
    }
}
