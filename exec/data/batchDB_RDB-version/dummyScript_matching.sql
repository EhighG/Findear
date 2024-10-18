-- dummy insert query - matchings
use findear_batchdb;
show variables like '%foreign%';
set foreign_key_checks = 1;

-- with lost112
set @lostBoardCnt = 5000000;
set @lost112AcquiredCnt = 1000000;
set @today = date_format(now(), '%Y-%m-%d');
select date_sub(@today, interval mod(10000 + 1, 10) day) ;


insert into tbl_lost112_matching(lost112_matching_id, lost_board_id, acquired_board_id, similarity_rate, matched_at)
with recursive lost112matchingdummy as
(
	select 1 as id, 1 as lostId, 1 as acquiredId, rand() as simRate, date_sub(@today, interval 5 day) as matchedDate
	union all
	select id + 1, lostId + 1, mod(acquiredId, @lost112AcquiredCnt) + 1, rand(), date_sub(@today, interval (5 - (id div 1000000)) day)
	from lost112matchingdummy
	where id < 5000000
)
select * from lost112matchingdummy;

select * from tbl_lost112_matching tlm  order by lost112_matching_id desc;
truncate tbl_lost112_matching ;

-- with findear

insert into tbl_findear_matching(findear_matching_id, lost_board_id, acquired_board_id, similarity_rate, matched_at)
with recursive findearmatchingdummy as
(
	select 1 as id, 1 as lostId, 1 as acquiredId, rand() as simRate, date_sub(@today, interval 5 day) as matchedDate
	union all
	select id + 1, lostId + 1, acquiredId + 1, rand(), date_sub(@today, interval (5 - (id div 1000000)) day)
	from findearmatchingdummy
	where id < 5000000
)
select * from findearmatchingdummy;



