package com.findear.main.board.query.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.findear.main.board.common.domain.QLostBoard.lostBoard;
import static org.assertj.core.api.Assertions.assertThat;

/** K-13: sortBy가 없거나 date가 아니어도 정렬 조건이 만들어져야 한다 (DB 불필요). */
class LostBoardQueryCustomRepositoryImplTest {

    @DisplayName("sortBy=date이면 분실일 기준 (방향은 desc 플래그)")
    @Test
    void sortByDate() {
        OrderSpecifier<?> asc = LostBoardQueryCustomRepositoryImpl.createOrder("date", false);
        OrderSpecifier<?> desc = LostBoardQueryCustomRepositoryImpl.createOrder("date", true);

        assertThat(asc.getTarget()).isEqualTo(lostBoard.lostAt);
        assertThat(asc.getOrder()).isEqualTo(Order.ASC);
        assertThat(desc.getTarget()).isEqualTo(lostBoard.lostAt);
        assertThat(desc.getOrder()).isEqualTo(Order.DESC);
    }

    @DisplayName("sortBy가 null이면 NPE 없이 id 기준")
    @Test
    void sortByNull() {
        OrderSpecifier<?> desc = LostBoardQueryCustomRepositoryImpl.createOrder(null, true);
        OrderSpecifier<?> asc = LostBoardQueryCustomRepositoryImpl.createOrder(null, false);

        assertThat(desc.getTarget()).isEqualTo(lostBoard.id);
        assertThat(desc.getOrder()).isEqualTo(Order.DESC);
        assertThat(asc.getTarget()).isEqualTo(lostBoard.id);
        assertThat(asc.getOrder()).isEqualTo(Order.ASC);
    }

    @DisplayName("sortBy가 date가 아닌 값이면 null을 돌려주지 않고 id 기준")
    @Test
    void sortByOther() {
        OrderSpecifier<?> order = LostBoardQueryCustomRepositoryImpl.createOrder("name", true);

        assertThat(order).isNotNull();
        assertThat(order.getTarget()).isEqualTo(lostBoard.id);
        assertThat(order.getOrder()).isEqualTo(Order.DESC);
    }
}
