package com.findear.main.board.common.domain;

import com.findear.main.board.command.dto.AiGeneratedColumnDto;
import com.findear.main.board.command.dto.ModifyAcquiredBoardReqDto;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "tbl_acquired_board")
public class AcquiredBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "acquired_board_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @OneToMany(mappedBy = "acquiredBoard", fetch = FetchType.LAZY)
    private List<ReturnLog> returnLogList = new ArrayList<>();

    @CreatedDate
    private LocalDate acquiredAt;

    private String address;

    private String name;

    private Float xPos;

    private Float yPos;

    public void updateAutoFilledColumn(AiGeneratedColumnDto aiGeneratedColumnDto) {
        this.board.updateAutofillColumn(aiGeneratedColumnDto);
    }

    public void modify(ModifyAcquiredBoardReqDto modifyReqDto) {
        if (modifyReqDto.getAcquiredAt() != null) {
            this.acquiredAt = LocalDate.parse(modifyReqDto.getAcquiredAt(), DateTimeFormatter.ISO_LOCAL_DATE);
        }
        // update board columns
        this.board.modify(modifyReqDto.getColor(), modifyReqDto.getImgFileList(), modifyReqDto.getCategory());
    }

    public void rollback() {
        this.getBoard().rollback();
    }
}
