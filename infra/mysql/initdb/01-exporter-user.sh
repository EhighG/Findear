#!/bin/bash
# mysqld-exporter 전용 계정 (R-12, docs/restoration/06-db-and-config.md §1)
# mysql 이미지 entrypoint가 데이터 디렉토리가 비어 있을 때(최초 초기화) 한 번만 실행한다.
# 호스트에 따라 이 파일이 실행 가능으로 보이기도 해서(Docker Desktop bind mount) entrypoint가
# source 하든 실행하든 동작하도록, entrypoint 내부 함수 대신 mysql 클라이언트를 직접 쓴다.
# 권한은 mysqld_exporter 문서의 권장값 (PROCESS, REPLICATION CLIENT, SELECT).

if [ -z "${MYSQL_EXPORTER_PASSWORD:-}" ]; then
	echo '[initdb] MYSQL_EXPORTER_PASSWORD가 비어 있습니다' >&2
	exit 1
fi

mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<EOSQL
CREATE USER IF NOT EXISTS 'exporter'@'%' IDENTIFIED BY '${MYSQL_EXPORTER_PASSWORD}' WITH MAX_USER_CONNECTIONS 3;
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';
EOSQL
