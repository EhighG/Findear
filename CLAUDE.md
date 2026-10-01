# CLAUDE.md — Findear

분실물 통합 관리 플랫폼 Findear. 2024 SSAFY 팀 프로젝트 → 2024 말 개인 리팩토링 → **2026-09부터 복구(재구성) 작업 중.**

## 세션 시작 시
1. `docs/restoration/README.md` → `docs/restoration/08-work-plan.md`(진행 상황) → `docs/restoration/10-worklog.md`(최근 로그) 순서로 읽는다.
2. 작업은 `08-work-plan.md`의 R-xx 단위로, 선행 작업 순서대로 진행한다.
3. 새로 정할 것이 생기면 README의 요구사항 P1~P8로 판단되는 경우만 결정하고 `03-decisions.md`에 D-번호로 기록한다. 판단이 안 되거나 유료 서비스가 필요하면 사용자에게 묻는다.

## 작업 방식 (역할 분담, D-46)
메인 세션은 오케스트레이터다: R-xx를 **작업 지시서**로 만들어 실행·검증을 subagent에 맡기고, git·이슈·진행 문서·결정(03)은 직접 관리한다. 실행은 `findear-executor`(Sonnet 5.5 high), 검증은 `findear-verifier`(Opus 5.5 high)가 맡는다 (`.claude/agents/`, Agent 호출 때 `model`은 넘기지 않는다).

R-xx마다:
1. 메인: master에서 브랜치를 만들고 작업 지시서를 쓴다.
2. executor: 지시서대로 수행하고 보고한다.
3. verifier: 같은 지시서와 executor 보고서를 받아 독립 검증한다.
4. FAIL이면 메인이 고칠 것만 담은 지시서로 2~3을 반복한다. 두 번 반복해도 FAIL이면 메인이 원인을 직접 조사해 처리하거나 사용자에게 보고한다.
5. PASS면 메인이 diff 확인 → 커밋 → `08-work-plan.md` 상단 절차(비밀값 검사, push, 이슈 참조, master 반영) → 08·10·README 갱신.

작업 지시서는 대화 맥락 없이 읽혀야 한다 (subagent는 CLAUDE.md만 공유한다). 담을 것: 목표와 관련 R-xx·D-xx, 읽을 문서 위치, 바꿀 파일과 구체적 내용, 범위 밖(손대지 않을 것), 완료 기준과 확인 명령·기대 결과, docker 기동 범위와 정리 방법(`down` / `down -v`).
메인이 직접 하는 것: git·이슈 작업, 진행 문서 갱신, 몇 줄짜리 수정. 병렬 위임은 docker·포트·파일이 겹치지 않는 작업끼리만.

## 세션 종료 시
- 세션은 Phase 단위다. Phase가 끝나면 멈추고 사용자에게 보고한다 (D-35).
- `08-work-plan.md` 체크박스·상태, `10-worklog.md` 로그, `docs/restoration/README.md`의 "현재 상태"를 갱신한다.
- 코드와 문서가 달라졌으면 문서를 고친다.

## 절대 규칙
- **public 레포.** 키·비밀번호·토큰·서비스계정 JSON·인증서 개인키를 커밋하지 않는다. `.env`, `secrets/`는 git 제외. push 전 `08-work-plan.md`의 "비밀값 검사"를 한다 (Push protection이 아직 꺼져 있음, D-37).
- **원본 레포 `2TF4/findear`(포크 원본)에는 이슈·PR·push·코멘트 등 어떤 쓰기도 하지 않는다** (D-36). gh 대상은 `.claude/settings.json`의 `GH_REPO=EhighG/Findear`로 고정돼 있고 `2TF4`가 들어간 명령은 차단된다. 이 설정을 지우거나 우회하지 말고, 원본 레포 remote(`upstream` 등)를 추가하지 않는다.
- `old-master` 브랜치(팀 종료 시점 `2af1413`)는 원본 보존용. 삭제·수정 금지.
- match의 AI 기능(OpenAI, fastText, Selenium)은 복구하지 않는다. mock만 만든다.
- `front/`는 1차 범위 밖. 수정하지 않는다.
- 개발 중(R-00~R-80)에는 **compose 전체를 한 번에 띄우지 않고 자원 실측도 하지 않는다** (D-32). 필요한 서비스만 부분 기동하고 확인 후 `docker compose down`. 전체 기동·실측은 최종 검증 R-90에서만.
- **외부 API(Naver 로그인, VWorld, Firebase/FCM, 공공데이터포털 Lost112, AWS)는 작업·검증 중 호출하지 않는다** (D-38, 키 없이 보내는 요청 포함). 공식 문서 열람은 허용. 공식 문서 기준으로 구현하고 mock 서버 계약 테스트로 검증해, 사용자가 마지막에 키만 세팅하면 바로 동작하게 만든다. 확인한 문서는 `05-external-integrations.md` §8에 기록. AWS(S3·IAM·EC2)는 실제 연결이 필요한 검증 자체를 생략하고 로컬(SeaweedFS)·문법 검사까지만 한다 (D-41).
- 컨테이너 설정은 일반적인 사용 방식을 유지한다 (D-31): GC 방식 변경, ES 기능 끄기, `GOMEMLIMIT` 같은 추가 튜닝을 하지 않는다.

## 레포 규칙 (README의 리팩토링 규칙 + D-33, D-34)
- Phase마다 이슈 1개(상위 이슈 #12 "Findear 복구 1차"의 sub-issue, `gh issue create --parent 12`), R-xx마다 master에서 브랜치 `{feature|fix|test}/{Phase 이슈번호}-{이름}`. master에서 분기한 브랜치만 원격에 push.
- 커밋 메시지 `Type: 한국어 설명` (Feat, Fix, Refactor, Chore, Docs, Test, Rename, Style, Comment). 이슈번호(`Related to #N`)는 master에 올라가는 커밋에만 — 작업 브랜치 push 후 로컬에서 `tools/git/add-issue-ref.sh`로 트레일러 앞에 붙이고(D-39) master에 fast-forward 병합·push, 로컬 브랜치 삭제. PR은 쓰지 않는다. 상세 절차는 `08-work-plan.md` 상단.
- Claude Code web 세션은 세션이 지정한 브랜치를 쓴다.

## 빌드 메모
- JDK 17+ (JDK 21에서 빌드 확인). `gradlew` 실행 권한이 없으면 `sh ./gradlew …`.
- main 단위 테스트(DB 불필요): `cd main && sh ./gradlew test --tests 'com.findear.main.board.query.service.LostBoardQueryServiceTest'`
- main·batch 전체 테스트(`sh ./gradlew test`)는 Testcontainers(MySQL·Redis / MySQL·ES)를 써서 Docker가 필요하다.
- Maven Central 429 발생 시 잠시 후 `--max-workers=1`로 재시도.
- 로컬 개발 PC(Windows)는 `core.autocrlf=true`. 컨테이너에서 실행할 `.sh`·`gradlew`는 `.gitattributes`로 LF 고정 (R-02).
- 로컬 개발 PC(Windows)에서 `python3`는 Microsoft Store 별칭이라 실행되지 않는다(exit 49). `python`(3.14)을 쓴다.
- 로컬 개발 PC의 gh는 fine-grained PAT(2026-10-17 만료, U-10), git push는 Git Credential Manager 자격증명을 쓴다.
- compose 검증: `cp .env.example .env` → `docker compose config --quiet`. 부분 기동 예: `docker compose up -d --build mysql flyway redis main`.
- 로컬 개발 PC에는 Windows용 MySQL 8.0 서비스(`MySQL80`)가 3306을 쓰고 있어 `.env`에 `MYSQL_HOST_PORT=3307` (D-44). 호스트 8080은 다른 프로젝트 컨테이너(`simple_board3`)가 써서 `MAIN_HOST_PORT=8090` → 호스트에서 main은 `localhost:8090`. 호스트 8082도 다른 프로젝트가 쓸 수 있어 `BATCH_HOST_PORT=8092` → batch는 `localhost:8092`. 다른 프로젝트 컨테이너도 떠 있을 수 있으니 이 프로젝트(`findear`) 것만 다룬다.
- Git Bash에서 docker 명령에 컨테이너 안 경로(`/usr/bin/...` 등)를 넘길 때는 `MSYS_NO_PATHCONV=1`을 붙인다 (안 붙이면 Windows 경로로 바뀜).
