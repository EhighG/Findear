package com.findear.main.board.query.service;

import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.ImgFile;
import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.board.query.dto.LostBoardDetailResDto;
import com.findear.main.board.query.repository.LostBoardQueryRepository;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class LostBoardQueryServiceTest {

    private LostBoardQueryService lostBoardQueryService;

    @Mock
    private LostBoardQueryRepository lostBoardQueryRepository;
    private AutoCloseable closeable;

    @BeforeEach
    void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        lostBoardQueryService = new LostBoardQueryServiceImpl(lostBoardQueryRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @DisplayName("분실물 ID로 조회")
    @Test
    void findById() {
        // given
        doReturn(foundLostBoardOptional()).when(lostBoardQueryRepository)
                .findById(any(Long.class));
        
        // when
        LostBoardDetailResDto foundLostBoard = lostBoardQueryService.findById(1L);

        // then
        assertThat(foundLostBoard.getLostBoardId()).isNotNull();
        assertThat(foundLostBoard.getBoard().getId()).isNotNull();
        assertThat(foundLostBoard.getBoard().getMember().getMemberId()).isNotNull();
    }

    @DisplayName("없는 ID로 조회 시, 예외 발생")
    @Test
    void findByIdTest2() {
        doReturn(Optional.empty()).when(lostBoardQueryRepository)
                .findById(any(Long.class));

        Assertions.assertThrows(Exception.class, () -> lostBoardQueryService.findById(1L));
    }

    private Optional<LostBoard> foundLostBoardOptional() {
        Member writer = Member.builder()
                .id(1L)
                .phoneNumber("testPhoneNumber")
                .role(Role.NORMAL)
                .build();
        Board foundBoard = Board.builder()
                .id(1L)
                .isLost(true)
                .member(writer)
                .productName("testProductName")
                .categoryName("testCategoryName")
                .imgFileList(List.of(new ImgFile(1L, "testImgUrl1")))
                .color("testColor")
                .registeredAt(LocalDateTime.now())
                .build();

        return Optional.of(
                LostBoard.builder()
                        .id(1L)
                        .board(foundBoard)
                        .lostAt(LocalDate.now())
                        .suspiciousPlace("testPlace")
                        .xPos(9.99f)
                        .yPos(9.99f)
                        .build()
        );
    }
}