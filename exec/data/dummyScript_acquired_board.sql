-- dummy insert query - Acquired(Findear) - 500만
set @memberIdCnt = 50000;
set @aiStringPrefix = 'sample AIdescription String ';
set @categoryPrefix = 'sampleCategoryName';
set @colorPrefix = 'sampleColor';
set @ProductNamePrefix = 'sampleProductName';
set @categoryCnt = 20;
set @colorCnt = 12;

-- Board first

insert into tbl_board(board_id, is_lost, member_id, ai_description, category_name, color, product_name)
with recursive boarddummy as 
(
	select 1 as id, false, 50001 as memberId, concat(@aiStringPrefix, cast(1 as char(7))) as aiDescription,
	concat(@categoryPrefix, cast(1 as char(7))) as categoryName, concat(@colorPrefix, cast(1 as char(7))) as color,
	concat(@ProductNamePrefix, cast(1 as char(7))) as productName
	union all
	select id + 1, false, mod(id + 1, @memberIdCnt) + @memberIdCnt, concat(@aiStringPrefix, id + 1),
	concat(@categoryPrefix, mod(id + 1, @categoryCnt)), concat(@colorPrefix, mod(id + 1, @colorCnt)),
	concat(@ProductNamePrefix, id + 1)
	from boarddummy
	where id < 5000000
)
select * from boarddummy;


truncate table tbl_board;
set foreign_key_checks = 0;
select * from tbl_board order by board_id desc limit 200;

-- 0-value check
update tbl_board
set category_name = 'sampleCategoryName1'
where category_name = 'sampleCategoryName0';

update tbl_board
set color = 'sampleColor1'
where color = 'sampleColor0';

-- fix lost's memberId, acquired's memberId
update tbl_board
set member_id = 1 where member_id = 0;

update tbl_board
set member_id = 50001 where member_id = 50000;