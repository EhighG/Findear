-- dummy insert query - Acquired(Findear) - 100만

use findear;

set cte_max_recursion_depth = 20000000;
set foreign_key_checks = 0;

set @memberIdCnt = 10000;
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
	select 1 as id, false, 10001 as memberId, concat(@aiStringPrefix, cast(1 as char(7))) as aiDescription,
	concat(@categoryPrefix, cast(1 as char(7))) as categoryName, concat(@colorPrefix, cast(1 as char(7))) as color,
	concat(@ProductNamePrefix, cast(1 as char(7))) as productName
	union all
	select id + 1, false, mod(id, @memberIdCnt) + 1 + @memberIdCnt, concat(@aiStringPrefix, id + 1),
	concat(@categoryPrefix, mod(id + 1, @categoryCnt)), concat(@colorPrefix, mod(id, @colorCnt) + 1),
	concat(@ProductNamePrefix, id + 1)
	from boarddummy
	where id < 1000000
)
select * from boarddummy;

select count(*) from tbl_board;

-- 0-value check
update tbl_board
set category_name = 'sampleCategoryName1'
where category_name = 'sampleCategoryName0';

update tbl_board
set color = 'sampleColor1'
where color = 'sampleColor0';
