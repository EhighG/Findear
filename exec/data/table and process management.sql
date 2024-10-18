use findear;
use findear_batchdb;

show variables like '%foreign%';

set foreign_key_checks = 0;
set foreign_key_checks = 1;

select count(*) from tbl_member;
select * from tbl_member;


truncate table tbl_member;

show full processlist;

kill 34;

-- TODO : 0 value checks for findear tables which use mod() in cte;