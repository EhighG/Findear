package com.findear.main.member.query.repository;

import com.findear.main.member.common.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberQueryRepository extends JpaRepository<Member, Long> {
    @Query("select m from Member m left join fetch m.agency where m.phoneNumber = :phoneNumber")
    Optional<Member> findByPhoneNumber(String phoneNumber);

    @Query("select m from Member m left join fetch m.agency where m.id = :memberId")
    Optional<Member> findByIdWithAgency(Long memberId);

//    @Query("select m from Member m left join fetch m.agency where m.withdrawalYn != true")
//    List<Member> findAll(String keyword);
}
