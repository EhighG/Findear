package com.findear.main.board.command.repository;

import com.findear.main.board.common.domain.ImgFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImgFileRepository extends JpaRepository<ImgFile, Long> {

    Optional<ImgFile> findFirstByImgKey(String imgKey);

    boolean existsByImgKey(String imgKey);
}
