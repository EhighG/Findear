-- dummy insert query - Member
use findear;

set cte_max_recursion_depth = 20000000;
set foreign_key_checks = 0;

set @phonePrefix = '010-1234-12';
set @naverUidPrefix = 'sampleNaverUid';

insert into tbl_member(member_id, phone_number, naver_uid)
with recursive memberdummy as
(
	select 1 as id, concat(@phonePrefix, cast(1 as char(6))) as phone,
	concat(@naverUidPrefix, cast(1 as char(6))) as naverUid
	union all
	select id + 1, concat(@phonePrefix, id + 1), concat(@naverUidPrefix, id + 1)
	from memberdummy
	where id < 20000
)
select * from memberdummy;

truncate tbl_member;


select count(*) from tbl_member;
