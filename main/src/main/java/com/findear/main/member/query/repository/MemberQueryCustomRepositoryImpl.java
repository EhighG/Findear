package com.findear.main.member.query.repository;

import com.blazebit.persistence.querydsl.JPQLNextExpressions;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.QMember;

import com.findear.main.member.common.domain.Role;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MemberQueryCustomRepositoryImpl implements MemberQueryCustomRepository{

    private final JPAQueryFactory jpaQueryFactory;

    public MemberQueryCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<Member> findFirstMembersGroupByRoleOrderById() {
        QMember member = QMember.member;
        List<Tuple> result = jpaQueryFactory
                .select(member,
                        JPQLNextExpressions.rowNumber().over().partitionBy(member.role).orderBy(member.id.asc())
                                .as("rNum"))
                .from(member)
                .orderBy(Expressions.stringPath("rNum").asc())
                .limit(Role.values().length) // role의 타입 수만큼 반환한다.
                .fetch();

        return result.stream()
                .map(row -> row.get(0, Member.class))
                .toList();
    }
}
