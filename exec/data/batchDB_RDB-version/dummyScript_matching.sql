-- dummy insert query - matchings
use findear_batchdb;

set cte_max_recursion_depth = 20000000;
set foreign_key_checks = 0;

-- with lost112
set @lostBoardCnt = 1000000;
set @memberCnt = 10000;
set @lost112AcquiredCnt = 1000000;
set @findearAcquiredCnt = 1000000;
set @today = date_format(now(), '%Y-%m-%d');

insert into tbl_lost112_matching(lost112_matching_id, lost_board_id, lost_member_id, acquired_board_id, similarity_rate, matched_at)
with recursive lost112matchingdummy as
(
	select 1 as id, 1 as lostId, 1 as lostMemberId, 1 as acquiredId, rand() as simRate, date_sub(@today, interval 5 day) as matchedDate
	union all
	select id + 1, mod(lostId, @lostBoardCnt) + 1, mod(lostMemberId, @memberCnt) + 1, mod(acquiredId, @lost112AcquiredCnt) + 1, rand(), date_sub(@today, interval (5 - (id div 1000000)) day)
	from lost112matchingdummy
	where id < 2000000
)
select * from lost112matchingdummy;

-- with findear

insert into tbl_findear_matching(findear_matching_id, lost_board_id, lost_member_id, acquired_board_id, similarity_rate, matched_at)
with recursive findearmatchingdummy as
(
	select 1 as id, 1 as lostId, 1 as lostMemberId, 1 as acquiredId, rand() as simRate, date_sub(@today, interval 5 day) as matchedDate
	union all
	select id + 1, mod(lostId, @lostBoardCnt) + 1, mod(lostMemberId, @memberCnt) + 1, mod(acquiredId, @findearAcquiredCnt) + 1, rand(), date_sub(@today, interval (5 - (id div 1000000)) day)
	from findearmatchingdummy
	where id < 2000000
)
select * from findearmatchingdummy;

select count(*) from tbl_lost112_matching;
select count(*) from tbl_findear_matching;


