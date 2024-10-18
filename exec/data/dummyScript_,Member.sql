-- dummy insert query - Member

set @phonePrefix = '010-1234-12';
set @naverUidPrefix = 'sampleNaverUid';
declare naverUidPrefix varchar(45) default 'sampleNaverUid';

insert into tbl_member(member_id, phone_number, naver_uid)
with recursive memberdummy as
(
	select 1 as id, concat(@phonePrefix, cast(1 as char(6))) as phone,
	concat(@naverUidPrefix, cast(1 as char(6))) as naverUid
	union all
	select id + 1, concat(@phonePrefix, id + 1), concat(@naverUidPrefix, id + 1)
	from memberdummy
	where id < 100000
)
select * from memberdummy;

select * from tbl_member;
select count(*) from tbl_member;
select rand();