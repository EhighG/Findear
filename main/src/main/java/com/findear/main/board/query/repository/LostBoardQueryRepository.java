package com.findear.main.board.query.repository;

import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.board.query.dto.LostBoardListResDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LostBoardQueryRepository extends JpaRepository<LostBoard, Long> {

    @Query("select lb from LostBoard lb join fetch lb.board left join fetch lb.board.imgFileList where lb.board.id = :boardId and lb.board.deleteYn = false")
    Optional<LostBoard> findByBoardId(Long boardId);

    @Query("select lb from LostBoard lb join fetch lb.board left join fetch lb.board.imgFileList where lb.id = :lostBoardId and lb.board.deleteYn = false")
    Optional<LostBoard> findById(Long lostBoardId);

    // TODO: findAll 쿼리 성능 개선 필요

    // delete_yn 기본값 적용 시 where 조건 바꾸기
    @Query("select new com.findear.main.board.query.dto.LostBoardListResDto(lb.id, b.id, b.productName, b.categoryName, b.thumbnailUrl, lb.lostAt, m.id, m.phoneNumber, lb.suspiciousPlace) " +
            "from LostBoard lb " +
            "join Board b on b.id = lb.board.id " +
            "join Member m on m.id = b.member.id " +
            "left join ImgFile if on if.board.id = b.id " +
            "where b.deleteYn = false ")
    List<LostBoardListResDto> findAllWithDtoForm();

    @Query("select new com.findear.main.board.query.dto.LostBoardListResDto(lb.id, b.id, b.productName, b.categoryName, b.thumbnailUrl, lb.lostAt, m.id, m.phoneNumber, lb.suspiciousPlace) " +
            "from LostBoard lb " +
            "join Board b on b.id = lb.board.id " +
            "join Member m on m.id = b.member.id " +
            "left join ImgFile if on if.board.id = b.id " +
            "where b.deleteYn = false " +
            "order by lb.lostAt")
    List<LostBoardListResDto> findAllOrderByLostAt();

    @Query("select new com.findear.main.board.query.dto.LostBoardListResDto(lb.id, b.id, b.productName, b.categoryName, b.thumbnailUrl, lb.lostAt, m.id, m.phoneNumber, lb.suspiciousPlace) " +
            "from LostBoard lb " +
            "join Board b on b.id = lb.board.id " +
            "join Member m on m.id = b.member.id " +
            "left join ImgFile if on if.board.id = b.id " +
            "where b.deleteYn = false " +
            "order by lb.lostAt desc")
    List<LostBoardListResDto> findAllOrderByLostAtDesc();

}
