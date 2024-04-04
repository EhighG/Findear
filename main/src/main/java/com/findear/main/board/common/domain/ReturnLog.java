package com.findear.main.board.common.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "tbl_return_log")
public class ReturnLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acquired_board_id")
    private AcquiredBoard acquiredBoard;

    private String phoneNumber;

    @CreatedDate
    private LocalDateTime returnedAt;

    private LocalDateTime cancelAt;

    public ReturnLog(AcquiredBoard acquiredBoard, String phoneNumber) {
        this.acquiredBoard = acquiredBoard;
        this.phoneNumber = phoneNumber;
    }

    public void rollback() {
        this.cancelAt = LocalDateTime.now();
    }
}
