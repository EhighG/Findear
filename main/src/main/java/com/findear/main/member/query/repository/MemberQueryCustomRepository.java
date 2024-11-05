package com.findear.main.member.query.repository;

import com.findear.main.member.common.domain.Member;

import java.util.List;

public interface MemberQueryCustomRepository {

    List<Member> findFirstMembersGroupByRoleOrderById();
}
