-- dummy insert query - scrap
use findear;

set cte_max_recursion_depth = 20000000;
set foreign_key_checks = 0;

insert into tbl_scrap(scrap_id, member_id, board_id)
with recursive scrapdummy as
(
	select 1 as id, 1 as memberId, 1 as boardId
	union all
	select id + 1, memberId + 1, boardId + 1
	from scrapdummy
	where id < 10000
)
select * from scrapdummy;


-- dummy insert query - scrap(lost112)

set @atcIdPrefix = 'sampleAtcId';

insert into tbl_lost112_scrap(lost112_scrap_id, member_id, lost112_atc_id)
with recursive lost112scrapdummy as
(
	select 1 as id, 1 as memberId, concat(@atcIdPrefix, cast(1 as char(5))) as atcId
	union all
	select id + 1, memberId + 1, concat(@atcIdPrefix, id + 1)
	from lost112scrapdummy
	where id < 10000
)
select * from lost112scrapdummy;

select count(*) from tbl_scrap ts;
select count(*) from tbl_lost112_scrap tls;