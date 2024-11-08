package com.findear.main.board.common.domain;

import com.findear.main.board.command.dto.AiGeneratedColumnDto;
import com.findear.main.board.command.dto.ModifyAcquiredBoardReqDto;
import com.findear.main.board.command.repository.ImgFileRepository;
import com.findear.main.member.common.domain.Member;
import com.findear.main.message.common.domain.MessageRoom;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "tbl_board")
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;

    @Column(nullable = false)
    private Boolean isLost;

    @Column(columnDefinition = "TEXT")
    private String aiDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "board", fetch = FetchType.LAZY)
    private List<MessageRoom> messageRoomList = new ArrayList<>();

    @OneToMany(mappedBy = "board", fetch = FetchType.LAZY)
    private List<Scrap> scrapList = new ArrayList<>();

    @OneToMany(mappedBy = "board", fetch = FetchType.LAZY)
    private List<ImgFile> imgFileList = new ArrayList<>();

    private String color;

    @OneToOne(mappedBy = "board", fetch = FetchType.LAZY)
    private AcquiredBoard acquiredBoard;

    @OneToOne(mappedBy = "board", fetch = FetchType.LAZY)
    private LostBoard lostBoard;

    private String productName;

    @Column(name = "status")
    @Enumerated(value = EnumType.STRING)
    private BoardStatus status;

    @Column(nullable = false)
    @ColumnDefault("0")
    private Boolean deleteYn;

    @CreatedDate
    private LocalDateTime registeredAt;

    private String thumbnailUrl;

    private String categoryName;

    public void updateImgFiles(List<ImgFile> imgFiles) {
        this.imgFileList = imgFiles;
    }

    public void updateAutofillColumn(AiGeneratedColumnDto aiGeneratedColumnDto) {
        this.categoryName = aiGeneratedColumnDto.getCategory();
        this.color = aiGeneratedColumnDto.getColor();
//        this.aiDescription = aiGeneratedColumnDto.getDescription();
        StringBuilder aiDescStr = new StringBuilder();
        List<String> generatedKeywords = aiGeneratedColumnDto.getDescription();
        for (String keyword : generatedKeywords) {
            aiDescStr.append(keyword).append(" ");
        }
        this.aiDescription = aiDescStr.substring(0, aiDescStr.length() - 1);
    }

    public void modify(String color, List<ImgFile> imgFileList, String category) {
        if (color != null) {
            this.color = color;
        }
        if (imgFileList != null) {
            this.imgFileList = imgFileList;
            this.thumbnailUrl = imgFileList.get(0).getImgUrl();
        }
        if (category != null) {
            this.categoryName = category;
        }
    }

    public void remove() {
        this.deleteYn = true;
    }

    public void giveBack() {
        this.status = BoardStatus.DONE;
    }

    public void rollback() {
        this.status = BoardStatus.ONGOING;
    }
}
