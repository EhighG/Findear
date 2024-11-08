-- dummy insert query - Acquired(Findear)
-- acquiredBoard

use findear;

set cte_max_recursion_depth = 20000000;
set foreign_key_checks = 0;

set @acquireDateStart = '2024-01-01';
set @AgencyCnt = 10000;
set @addrPrefix = 'sampleAddressString';
set @namePrefix = 'sampleName';

insert into tbl_acquired_board(acquired_board_id, board_id, acquired_at, address, x_pos, y_pos, name)
with recursive dummyAcquired as
(
	select 1 as id, 1 as boardId, now() as acquiredAt, concat(@addrPrefix, cast(1 as char(7))) as addr,
	rand() as xPos, rand() as yPos, concat(@namePrefix, cast(1 as char(7))) as name
	union all
	select id + 1, id + 1, now(), concat(@addrPrefix, id + 1), rand(), rand(), concat(@namePrefix, id + 1)
	from dummyAcquired
	where id < 1000000
)
select * from dummyAcquired;

select count(*) from tbl_acquired_board tab ;