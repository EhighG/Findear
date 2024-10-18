-- dummy insert query - acquired(lost112); for BatchDB
-- 500만개

use findear_batchdb;

set @atcIdPrefix = 'sampleAtcId';
set @depPlacePrefix = 'sampleDepPlace';
set @addrPrefix = 'sampleAddressString';
set @imgUrlPrefix = 'sampleUrl_sampleUrl_sampleUrl_sampleUrl_sampleUrl_sampleUrl_sampleUrl_sampleUrl_sampleUrl_sampleUrl_sampleUrl_sampleUrl_sampleUrl_sampleUrl_sampleUrl_sampleUrl_sampleUrl_sampleUrl_sampleUrl_sampleUrl_';
set @productNamePrefix = 'sampleProductName';
set @subjectPrefix = 'sampleSubject';
set @colorPrefix = 'color';
set @acDate = now();
set @clNmPrefix = 'category';
set @mainClPrefix = 'mainCategory';
set @subClPrefix = 'subCategory';

set foreign_key_checks = 0;

insert into tbl_lost112_acquired(lost112_acquired_id, atc_id, dep_place, addr, fd_file_path_img, fd_prdt_nm, fd_sbjt, clr_nm, fd_ymd, prdt_cl_nm, main_prdt_cl_nm, sub_prdt_cl_nm)
with recursive lost112acquireddummy as
(
	select 1 as id,
	concat(@atcIdPrefix, cast(1 as char(7))) as atcId,
	concat(@depPlacePrefix, cast(1 as char(7))) as depPlace,
	concat(@addrPrefix, cast(1 as char(7))) as addr,
	concat(@imgUrlPrefix, cast(1 as char(7))) as imgUrl,
	concat(@productNamePrefix, cast(1 as char(7))) as productName,
	concat(@subjectPrefix, cast(1 as char(7))) as subject,
	concat(@colorPrefix, cast(1 as char(7))) as color,
	@acDate as acquiredDate,
	concat(@clNmPrefix, cast(1 as char(7))) as clNm,
	concat(@mainClPrefix, cast(1 as char(7))) as mainCl,
	concat(@subClPrefix, cast(1 as char(7))) as subCl
	union all
	select id + 1,
	concat(@atcIdPrefix, id + 1),
	concat(@depPlacePrefix, id + 1),
	concat(@addrPrefix, id + 1),
	concat(@imgUrlPrefix, id + 1),
	concat(@productNamePrefix, id + 1),
	concat(@subjectPrefix, id + 1),
	concat(@colorPrefix, mod(id + 1, 10) + 1),
	@acDate,
	concat(@clNmPrefix, mod(id + 1, 20) + 1),
	concat(@mainClPrefix, mod(id + 1, 20) + 1),
	concat(@subClPrefix, mod(id + 1, 30) + 1)
	from lost112acquireddummy
	where id < 1000000
)
select * from lost112acquireddummy;


select count(*) from tbl_lost112_acquired;
truncate tbl_lost112_acquired;