-- dummy insert query - return log -> acquired 중 1/2 (=250만 개)
use findear;

set cte_max_recursion_depth = 20000000;
set foreign_key_checks = 0;

set @nw = now();
set @phonePrefix = '010-1234-12';

insert into tbl_return_log(return_log_id, acquired_board_id, returned_at, phone_number)
with recursive returnlogdummy as
(
	select 1 as id, 1 as acquiredId, @nw as returnDate, 1 as tmpMemberId,
	concat(@phonePrefix, cast(1 as char(5))) as phone
	union all
	select id + 1, acquiredId + 1, @nw, mod(tmpMemberId, 10000) + 1,
	concat(@phonePrefix, mod(tmpMemberId, 10000) + 1)
	from returnlogdummy
	where id < 500000
)
select id, acquiredId, returnDate, phone
from returnlogdummy;

set @newnow = now();

-- cancel : 250,000
update tbl_return_log
set cancel_at = @newnow
where return_log_id <= 250000;

-- re-return : 125,000
insert into tbl_return_log(return_log_id, acquired_board_id, returned_at, phone_number)
with recursive returnlogdummy as
(
	select 500001 as id, 500001 as acquiredId, @nw as returnDate, 1 as tmpMemberId,
	concat(@phonePrefix, cast(1 as char(5))) as phone
	union all
	select id + 1, acquiredId + 1, @nw, mod(tmpMemberId, 10000) + 1,
	concat(@phonePrefix, mod(tmpMemberId, 10000) + 1)
	from returnlogdummy
	where id < 625000
)
select id, acquiredId, returnDate, phone
from returnlogdummy;

select count(*) from tbl_return_log;

