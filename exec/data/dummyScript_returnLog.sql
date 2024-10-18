-- dummy insert query - return log -> acquired 중 1/2 (=250만 개)
set @nw = now();
set @phonePrefix = '010-1234-12';
set foreign_key_checks = 0;

insert into tbl_return_log(return_log_id, acquired_board_id, returned_at, phone_number)
with recursive returnlogdummy as
(
	select 1 as id, 1 as acquiredId, @nw as returnDate, 1 as tmpMemberId,
	concat(@phonePrefix, cast(1 as char(5))) as phone
	union all
	select id + 1, acquiredId + 1, @nw, mod(tmpMemberId, 50000) + 1,
	concat(@phonePrefix, mod(tmpMemberId, 50000) + 1)
	from returnlogdummy
	where id < 2500000
)
select id, acquiredId, returnDate, phone
from returnlogdummy;

set @newnow = now();

-- cancel : 1,250,000
update tbl_return_log
set cancel_at = @newnow
where return_log_id <= 1250000;

-- re-return : 625,000
insert into tbl_return_log(return_log_id, acquired_board_id, returned_at, phone_number)
with recursive returnlogdummy as
(
	select 2500001 as id, 2500001 as acquiredId, @nw as returnDate, 1 as tmpMemberId,
	concat(@phonePrefix, cast(1 as char(5))) as phone
	union all
	select id + 1, acquiredId + 1, @nw, mod(tmpMemberId, 50000) + 1,
	concat(@phonePrefix, mod(tmpMemberId, 50000) + 1)
	from returnlogdummy
	where id < 3125000
)
select id, acquiredId, returnDate, phone
from returnlogdummy;

select phone_number
from tbl_return_log trl
group by phone_number
order by return_log_id;

