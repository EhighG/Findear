package com.findear.main.board.command.service;

import com.findear.main.board.command.repository.ImgFileRepository;
import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.ImgFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 게시글 수정 때 이미지 목록을 요청한 imgKeys와 정확히 같게(순서 포함) 맞춘다 (K-14).
 *
 * 조회는 tbl_img_file을 id 순으로 읽으므로 표시 순서 = id 순서다. 그래서 기존 행 중 요청 목록의 앞부분과
 * 같은 key(같은 순서)로 이어지는 행만 그대로 두고, 그 뒤의 기존 행은 삭제한 뒤 나머지 key를 순서대로 새로 저장한다.
 * (끝에 추가하거나 끝에서 제거하는 흔한 수정은 기존 행이 유지되고, 순서를 바꾸면 바뀐 지점부터 다시 만든다.)
 * 스토리지의 옛 객체는 지우지 않는다 (고아 객체 정리는 별도 작업).
 *
 * 삭제는 Board.imgFileList의 orphanRemoval 대신 리포지토리로 명시한다: 등록·수정 코드가 컬렉션을 통째로 교체하는 방식이라
 * (updateImgFiles, Board.modify) orphanRemoval을 켜면 컬렉션 교체가 예외("no longer referenced")를 일으키기 때문이다.
 */
final class ImgFileSync {

    private ImgFileSync() {
    }

    /** 반환값은 수정 후 게시글의 이미지 목록(요청 순서). 다른 게시글에 붙은 key가 있으면 IllegalArgumentException */
    static List<ImgFile> sync(Board board, List<String> keys, ImgFileRepository imgFileRepository) {
        List<ImgFile> existing = new ArrayList<>(board.getImgFileList());
        existing.sort(Comparator.comparing(ImgFile::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        int keep = 0;
        while (keep < existing.size() && keep < keys.size()
                && keys.get(keep).equals(existing.get(keep).getImgKey())) {
            keep++;
        }
        List<ImgFile> toDelete = new ArrayList<>(existing.subList(keep, existing.size()));

        // 삭제·저장 전에 소유 확인: 새로 붙일 key가 이 게시글의 (곧 삭제할) 행이 아닌 다른 행에 있으면 다른 게시글의 이미지다
        for (String key : keys.subList(keep, keys.size())) {
            imgFileRepository.findFirstByImgKey(key).ifPresent(found -> {
                if (toDelete.stream().noneMatch(old -> isSame(old, found))) {
                    throw new IllegalArgumentException("이미 다른 게시글에서 사용 중인 이미지입니다.");
                }
            });
        }

        if (!toDelete.isEmpty()) {
            imgFileRepository.deleteAll(toDelete);
        }
        List<ImgFile> result = new ArrayList<>(existing.subList(0, keep));
        for (String key : keys.subList(keep, keys.size())) {
            result.add(imgFileRepository.save(new ImgFile(board, key)));
        }
        return result;
    }

    private static boolean isSame(ImgFile a, ImgFile b) {
        return a == b || (a.getId() != null && a.getId().equals(b.getId()));
    }
}
