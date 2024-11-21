package com.findear.main.board.query.repository;

import com.findear.main.board.query.dto.FindAllLostBoardReqDto;
import com.findear.main.board.query.dto.LostBoardListResDto;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.util.List;

import static com.findear.main.board.common.domain.QBoard.board;
import static com.findear.main.board.common.domain.QImgFile.imgFile;
import static com.findear.main.board.common.domain.QLostBoard.lostBoard;
import static com.findear.main.member.common.domain.QMember.member;

public class LostBoardQueryCustomRepositoryImpl implements LostBoardQueryCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public LostBoardQueryCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    // TODO: 쿼리 성능 개선 필요
    @Override
    public Page<LostBoardListResDto> findAllWithDtoForm(FindAllLostBoardReqDto findAllReq, Pageable pageable) {
        List<LostBoardListResDto> contents = jpaQueryFactory.select(Projections.fields(LostBoardListResDto.class,
                        lostBoard.id,
                        board.id,
                        board.productName,
                        board.categoryName,
                        board.thumbnailUrl,
                        lostBoard.lostAt,
                        member.id,
                        member.phoneNumber,
                        lostBoard.suspiciousPlace))
                .from(lostBoard)
                .join(lostBoard.board, board)
                .join(board.member, member)
                .leftJoin(board.imgFileList, imgFile)
                .where(board.deleteYn.not(),
                        memberIdEq(findAllReq.getMemberId()),
                        categoryLike(findAllReq.getCategory()),
                        dateBetween(findAllReq.getSDate(), findAllReq.getEDate()),
                        productNameLike(findAllReq.getKeyword()))
                .orderBy(createOrder(findAllReq.getSortBy(), findAllReq.isDesc()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(lostBoard.count())
                .from(lostBoard)
                .join(lostBoard.board, board)
                .join(board.member, member)
                .leftJoin(board.imgFileList, imgFile)
                .where(board.deleteYn.not(),
                        memberIdEq(findAllReq.getMemberId()),
                        categoryLike(findAllReq.getCategory()),
                        dateBetween(findAllReq.getSDate(), findAllReq.getEDate()),
                        productNameLike(findAllReq.getKeyword()));

        return PageableExecutionUtils.getPage(contents, pageable, countQuery::fetchOne);
    }


    private BooleanExpression memberIdEq(Long memberId) {
        return memberId != null ? member.id.eq(memberId) : null;
    }

    private BooleanExpression categoryLike(String category) {
        return category != null ? board.categoryName.contains(category) : null;
    }

    // 둘 다 있거나, 둘 다 없다. (한쪽만 없을 땐 서비스 단에서 넣어준다.)
    private BooleanExpression dateBetween(LocalDate sDate, LocalDate eDate) {
        if (sDate == null && eDate == null) {
            return null;
        }
        return lostBoard.lostAt.between(sDate, eDate);
    }

    private BooleanExpression productNameLike(String keyword) {
        return keyword != null ? board.productName.contains(keyword) : null;
    }

    private OrderSpecifier<?> createOrder(String sortBy, boolean desc) {
        if (sortBy.equals("date")) {
            return new OrderSpecifier<>(desc ? Order.DESC : Order.ASC, lostBoard.lostAt);
        }
        return null;
    }
}
