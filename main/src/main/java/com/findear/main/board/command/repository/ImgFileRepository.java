package com.findear.main.board.command.repository;

import com.findear.main.board.common.domain.ImgFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImgFileRepository extends JpaRepository<ImgFile, Long> {

    // 개발환경용. 같은 imgUrl이 여러 번 들어갈 때를 위한 메소드
    Optional<ImgFile> findFirstByImgUrl(String imgUrl);
    Optional<ImgFile> findByImgUrl(String imgUrl);
}
