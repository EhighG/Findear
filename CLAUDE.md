# CLAUDE.md — Findear

분실물 통합 관리 플랫폼 Findear. 2024 SSAFY 팀 프로젝트 → 2024 말 개인 리팩토링 → **2026-09부터 복구(재구성) 작업 중.**

## 세션 시작 시
1. `docs/restoration/README.md` → `docs/restoration/08-work-plan.md`(진행 상황) → `docs/restoration/10-worklog.md`(최근 로그) 순서로 읽는다.
2. 작업은 `08-work-plan.md`의 R-xx 단위로, 선행 작업 순서대로 진행한다.
3. 새로 정할 것이 생기면 README의 요구사항 P1~P8로 판단되는 경우만 결정하고 `03-decisions.md`에 D-번호로 기록한다. 판단이 안 되거나 유료 서비스가 필요하면 사용자에게 묻는다.

## 세션 종료 시
- `08-work-plan.md` 체크박스·상태, `10-worklog.md` 로그, `docs/restoration/README.md`의 "현재 상태"를 갱신한다.
- 코드와 문서가 달라졌으면 문서를 고친다.

## 절대 규칙
- **public 레포.** 키·비밀번호·토큰·서비스계정 JSON·인증서 개인키를 커밋하지 않는다. `.env`, `secrets/`는 git 제외. 커밋 전 `git diff --cached` 확인.
- `old-master` 브랜치(팀 종료 시점 `2af1413`)는 원본 보존용. 삭제·수정 금지.
- match의 AI 기능(OpenAI, fastText, Selenium)은 복구하지 않는다. mock만 만든다.
- `front/`는 1차 범위 밖. 수정하지 않는다.

## 레포 규칙 (README의 리팩토링 규칙)
- 이슈 생성 후 작업. 브랜치 `{feature|fix|test}/{이슈번호}-{이름}`, master에서 분기한 브랜치만 원격에 push. Claude Code web 세션은 세션이 지정한 브랜치를 쓴다.
- 커밋 메시지 `Type: 한국어 설명` (Feat, Fix, Refactor, Chore, Docs, Test, Rename, Style, Comment). 이슈번호는 master에 올라가는 커밋에만.

## 빌드 메모
- JDK 17+ (JDK 21에서 빌드 확인). `gradlew` 실행 권한이 없으면 `sh ./gradlew …`.
- main 단위 테스트(DB 불필요): `cd main && sh ./gradlew test --tests 'com.findear.main.board.query.service.LostBoardQueryServiceTest'`
- Maven Central 429 발생 시 잠시 후 `--max-workers=1`로 재시도.
- 목표 구성 완료 후 전체 기동: `cp .env.example .env` → 값 채우기 → `docker compose up -d --build`.
