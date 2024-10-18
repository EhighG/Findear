-- dummy insert query - Lost - 500만
set foreign_key_checks = 0;

set @memberIdCnt = 50000;
set @aiStringPrefix = 'sample content looking for something ';
set @categoryPrefix = 'sampleCategoryName';
set @colorPrefix = 'sampleColor';
set @ProductNamePrefix = 'sampleProductName';
set @categoryCnt = 20;
set @colorCnt = 12;

-- Board first

insert into tbl_board(board_id, is_lost, member_id, ai_description, category_name, color, product_name)
with recursive boarddummy as 
(
	select 5000001 as id, true, 1 as memberId, concat(@aiStringPrefix, cast(5000001 as char(8))) as aiDescription,
	concat(@categoryPrefix, cast(5000001 as char(8))) as categoryName, concat(@colorPrefix, cast(5000001 as char(8))) as color,
	concat(@ProductNamePrefix, cast(5000001 as char(8))) as productName
	union all
	select id + 1, false, mod(id + 1, @memberIdCnt), concat(@aiStringPrefix, id + 1),
	concat(@categoryPrefix, mod(id + 1, @categoryCnt)), concat(@colorPrefix, mod(id + 1, @colorCnt)),
	concat(@ProductNamePrefix, id + 1)
	from boarddummy
	where id < 10000000
)
select * from boarddummy;

set @placePrefix = '분실 의심 지역 샘플 문자열 ';

insert into tbl_lost_board (lost_board_id, board_id, lost_at, x_pos, y_pos, suspicious_place)
with recursive lostboarddummy as
(
	select 1 as id, 5000001 as boardId, now() as lostAt, rand() as xPos, rand() as yPos, concat(@placePrefix, cast(1 as char(8))) as place
	union all
	select id + 1, boardId + 1, now(), rand(), rand(), concat(@placePrefix, id + 1)
	from lostboarddummy
	where id < 5000000
)
select * from lostboarddummy;


truncate table tbl_board;
set foreign_key_checks = 0;
select * from tbl_board order by board_id desc limit 200;
