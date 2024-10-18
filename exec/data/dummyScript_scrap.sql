-- dummy insert query - scrap
insert into tbl_scrap(scrap_id, member_id, board_id)
with recursive scrapdummy as
(
	select 1 as id, 1 as memberId, 1 as boardId
	union all
	select id + 1, memberId + 1, boardId + 1
	from scrapdummy
	where id < 50000
)
select * from scrapdummy;

select * from tbl_scrap ts;
select count(*) from tbl_scrap ts;

-- dummy insert query - scrap(lost112)

set @atcIdPrefix = 'sampleAtcId';

insert into tbl_lost112_scrap(lost112_scrap_id, member_id, lost112_atc_id)
with recursive lost112scrapdummy as
(
	select 1 as id, 1 as memberId, concat(@atcIdPrefix, cast(1 as char(5))) as atcId
	union all
	select id + 1, memberId + 1, concat(@atcIdPrefix, id + 1)
	from lost112scrapdummy
	where id < 50000
)
select * from lost112scrapdummy;