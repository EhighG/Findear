package com.findear.main.member.query.repository;

import com.findear.main.member.common.domain.Agency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgencyQueryRepository extends JpaRepository<Agency, Long> {
    @Query("select a from Agency a left join fetch a.memberList where a.address = :address and a.name = :name")
    Optional<Agency> findByAddressAndName(String address, String name);

}
