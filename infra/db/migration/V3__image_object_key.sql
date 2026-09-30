-- 이미지 컬럼을 URL 저장에서 object key 저장으로 정리한다 (D-13, D-47, R-24)
--   tbl_img_file.img_url    -> img_key        (images/{yyyy}/{MM}/{uuid}.{ext})
--   tbl_board.thumbnail_url -> thumbnail_key  (첫 이미지의 object key)
-- 타입(varchar(255))·NULL 여부는 그대로다. 응답 URL은 main이 STORAGE_PUBLIC_BASE_URL + "/" + key로 조립한다.
-- 이전에 URL이 저장된 행은 이 마이그레이션이 바꾸지 않는다 (개발 시드는 thumbnail을 NULL로 넣고 이미지는 없다).

ALTER TABLE tbl_img_file RENAME COLUMN img_url TO img_key;
ALTER TABLE tbl_board RENAME COLUMN thumbnail_url TO thumbnail_key;
