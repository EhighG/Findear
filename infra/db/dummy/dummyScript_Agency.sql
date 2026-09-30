-- dummy insert query - Agency

use findear;

set cte_max_recursion_depth = 20000000;
set foreign_key_checks = 0;

set @startMemberId = 10001;
set @addrPrefix = 'sampleAddressString';
set @namePrefix = 'sampleName';

insert into tbl_Agency(agency_id, x_pos, y_pos, address, name)
with recursive agencydummy as
(
	select 1 as id, rand() as xPos, rand() as yPos, concat(@addrPrefix, cast(1 as char(5))) as addr,
	concat(@namePrefix, cast(1 as char(5))) as name
	union all
	select id + 1, rand(), rand(), concat(@addrPrefix, id + 1), concat(@namePrefix, id + 1)
	from agencydummy
	where id < 10000
)
select * from agencydummy;

update tbl_member
set role = 'MANAGER', agency_id = member_id - 10000
where member_id >= @startMemberId;


select count(*) from tbl_agency;