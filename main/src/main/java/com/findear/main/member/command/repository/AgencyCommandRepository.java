package com.findear.main.member.command.repository;

import com.findear.main.member.common.domain.Agency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgencyCommandRepository extends JpaRepository<Agency, Long> {

}
