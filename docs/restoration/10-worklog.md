# 10. 작업 로그

> 세션이 끝날 때마다 맨 위에 추가하세요. 형식: 날짜 / 세션(환경·브랜치) / 한 일 / 남은 일·주의사항.

## 2026-09-29 — Claude Code on the web (`claude/happy-babbage-qt991n`)

**한 일**
- 전체 git 히스토리(845 커밋, 18 브랜치) 조사: 팀 종료 시점 인프라 인벤토리([01](01-legacy-inventory.md)), 유실 정보·보안 이슈 정리.
- 사용자와 복구 방향 합의: 요구사항 P1~P8, 결정 D-01~D-30 ([03](03-decisions.md)).
- 현재 master 상태 점검([02](02-current-state.md)): main은 JDK 21 + Gradle 8.5로 컴파일 성공, `LostBoardQueryServiceTest` 4/4 통과. 알려진 문제 K-01~K-11 기록.
- 목표 구성·리소스 산정·모니터링 설계([04](04-target-architecture.md)), 외부 연동([05](05-external-integrations.md)), DB·설정·환경변수([06](06-db-and-config.md)), API 계약·match mock 명세([07](07-api-contracts.md)), 작업 계획([08](08-work-plan.md)), 배포·AWS 키트 명세([09](09-deploy-and-aws.md)) 작성. 루트 `CLAUDE.md` 추가.
- 이미지 버전은 2026-09-29 Docker Hub 기준으로 확인 (MinIO 공식 이미지가 Docker Hub에서 사라진 것 확인 → SeaweedFS 선택).

**하지 않은 것**
- 코드·인프라 구현은 시작하지 않음. `Chore/10-reset_env` 브랜치 삭제(R-01)도 아직 안 함.

**다음 세션**
1. 사용자: U-01(Naver Secret 재발급), U-02, U-09(이 브랜치 master 병합), U-04~U-07 발급 미리 진행.
2. R-01 → R-02 → Phase 1부터 [08](08-work-plan.md) 순서대로.

**추가 (같은 날)**
- 1차 목표 최종 검증 시나리오 R-90을 [08](08-work-plan.md)에 추가, README의 다음 작업 순서 정리.

**주의**
- 클라우드 세션에서 Maven Central이 간헐적으로 429를 반환함 → 잠시 후 `--max-workers=1`로 재시도.
- `gradlew` 실행 권한이 없어 `sh ./gradlew`로 실행 (R-02에서 수정 예정).
