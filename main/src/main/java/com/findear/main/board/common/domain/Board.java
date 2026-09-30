package com.findear.main.board.common.domain;

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
import java.util.stream.Collectors;

@AllArgsConstructor
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "tbl_board", indexes =
        @Index(name = "ix_is_lost_delete_yn", columnList = "is_lost, delete_yn")
)
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

    @Builder.Default
    @OneToMany(mappedBy = "board", fetch = FetchType.LAZY)
    private List<MessageRoom> messageRoomList = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "board", fetch = FetchType.LAZY)
    private List<Scrap> scrapList = new ArrayList<>();

    @Builder.Default
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

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private Boolean deleteYn = false;

    @CreatedDate
    private LocalDateTime registeredAt;

    /** 첫 이미지의 object key */
    private String thumbnailKey;

    private String categoryName;

    public void updateImgFiles(List<ImgFile> imgFiles) {
        this.imgFileList = imgFiles;
    }

    /**
     * AI 자동채움 결과로 비어 있는 컬럼만 채운다 (D-52). null이거나 공백뿐인 컬럼만 채우고, 이미 값이 있으면(관리자가 먼저 수정) 그대로 둔다.
     * 키워드는 null·공백을 뺀 값을 앞뒤 공백 제거 후 " "로 이어 붙이며, 남는 값이 없으면 aiDescription을 바꾸지 않는다.
     */
    public void fillAutoColumns(String category, String color, List<String> keywords) {
        if (isBlank(this.categoryName) && !isBlank(category)) {
            this.categoryName = category;
        }
        if (isBlank(this.color) && !isBlank(color)) {
            this.color = color;
        }
        if (isBlank(this.aiDescription) && keywords != null) {
            String joined = keywords.stream()
                    .filter(keyword -> !isBlank(keyword))
                    .map(String::trim)
                    .collect(Collectors.joining(" "));
            if (!joined.isEmpty()) {
                this.aiDescription = joined;
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public void modify(String color, List<ImgFile> imgFileList, String category) {
        if (color != null) {
            this.color = color;
        }
        if (imgFileList != null) {
            this.imgFileList = imgFileList;
            this.thumbnailKey = imgFileList.isEmpty() ? null : imgFileList.get(0).getImgKey();
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
