package com.findear.batch.ours.repository;

import com.findear.batch.ours.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
