### 리팩토링시 규칙
#### 브랜치
- 브랜치 이름 : {타입}/{이슈번호}-{이름}
  - 예시) feature/8-lostboard-findall
  - 타입 : feature, fix, test 중 1
- remote 저장소에는 master에서 분기한 브랜치만 푸시
- 브랜치 master에 병합 시,
  remote의 해당 브랜치에 푸시 (세부 작업 내역 기록하기 위함)
  -> 로컬에서 (필요 시 변경 후) master에 병합
  -> remote master에 푸시
  -> 로컬 브랜치 삭제
  - remote의 브랜치에 세부 작업 내역 기록하기 위함

#### 이슈
- 기본적으로, 이슈 생성 후 작업 진행
##### 커밋 시 이슈 번호
- 이슈 번호는 master브랜치에 올라가는 커밋에만 붙이기
  - 이슈 창에서 관련 커밋 확인할때, 삭제된 커밋임에도 노출됨. master의 커밋들과 중복되어 보기 불편함
  - 브랜치 master에 병합 시, 로컬에서 master에 병합 전에 rebase 등으로 이슈번호 붙이기
#### 이슈와 브랜치
- 한 이슈에 여러 작업이 필요할 시, 1이슈 : n브랜치

---

# 분실물 통합 관리 플랫폼 Findear
<img src="https://github.com/yee950419/baekjoon/assets/65946607/786a6907-7406-4f2f-8c18-58d0ee9dbb73" width="600" height="300">

<hr>

## 📌 서비스 소개

##### 🤔 Why Findear?

- 많은 사람들이 물건을 잃어버리지만 물건을 찾는 일은 쉽지 않습니다.
- 시설의 관리자들은 고객이 놓고간 물건들을 보관하고 관리하는데 많은 리스크를 가지고 있습니다.
- Findear는 AI를 활용한 습득물 간편 등록 시스템과 인계 시스템, 분실,습득물의 통합조회 서비스 제공과 관리 기능과 함께 유사 습득물 매칭 서비스의 제공으로 기존의 문제점을 해결하고자 이 프로젝트를 시작하게 되었습니다.

##### 🧑‍🤝‍🧑 Easy & Safe Interaction

- 파인디어는 편의성과 안전성을 최우선으로 생각하였습니다
- 파인디어에선 간편로그인으로 빠르게 가입하고, 서비스 내에서 발생할 수 있는 위험을 방지합니다.

##### 🔄 For Loster

- 파인디어는 분실자들에게 분실물 등록 및 Lost112와의 통합 습득물 조회 기능을 제공합니다.
- 파인디어는 분실자가 올린 분실물 정보와 파인디어와 Lost112에 등록된 습득물 정보 간의 매칭시스템을 통해 유사한 습득물을 매칭해 빠르게 물건을 찾을 수 있도록 돕습니다.
- PWA를 적용해 웹푸시 알림을 제공하여 실시간으로 발생하는 이벤트 안내를 받을 수 있습니다.

##### 💻 For Manager

- 파인디어는 시설 관리자의 편의를 최대한으로 생각합니다.
- 파인디어에서 시설 관리자들은 습득한 물건의 사진과 물건명만 입력하면 AI를 이용하여 물건의 종류, 색상 정보 등을 분석하여 자동으로 채워주고
- 습득물의 관리와 안전 인계 기능을 제공합니다.

<br/>

## 👩 팀 구성

| [지인성](https://github.com/JIINSUNG)                                                     | [이상학](https://github.com/yee950419)                                                             | [손영훈](https://github.com/syhuni)                                                       | [신문영](https://github.com/ztrl)                                                         | [김동건](https://github.com/Zerotay)                                                      | [강이규](https://github.com/EhighG)                                                              |
| ----------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| <img src="https://avatars.githubusercontent.com/u/49591292?v=4" width="150" height="150"> | <img src="https://avatars.githubusercontent.com/u/65946607?v=4" width="150" height="150">          | <img src="https://avatars.githubusercontent.com/u/74291750?v=4" width="150" height="150"> | <img src="https://avatars.githubusercontent.com/u/88647858?v=4" width="150" height="150"> | <img src="https://avatars.githubusercontent.com/u/67823010?v=4" width="150" height="150"> | <img src="https://avatars.githubusercontent.com/u/71206505?v=4" width="150" height="150">        |
| Leader, Front                                                                             | Back                                                                                               | AI, Data                                                                                  | Front                                                                                     | Infra, AI                                                                                 | Back                                                                                             |
| PM <br/> 백그라운드, 실시간알림 <br/> 메인 서비스 구현 <br/>                              | Batch 기능 구현 <br>elastic search 검색 기능 구현<br>Ai Server와의 통신 api 구현<br>알림 기능 구축 | Django 서버 api 구현 <br/> GPT api 프롬프트 엔지니어링 <br/> 분실물 공공 데이터 정제      | 메인 화면 설계 구현 <br/> 사용자 상태 관리 <br/> 장소 데이터 API 연동 <br/>               | 서버 구축 및 관리 <br/> CI/CD <br/> 매칭 알고리즘 설계 및 구현 <br> 무중단 배포           | 회원 관리 및 인증/인가 <br>OAuth2 소셜 로그인<br>서버 간 비동기 요청 처리<br>Front 통신 api 구현 |

<br/>

## 🛠️ 기술 스택

Front
<br/>
<img src="https://img.shields.io/badge/typescript-3178C6?style=for-the-badge&logo=typescript&logoColor=black" width="auto" height="25">
<img src="https://img.shields.io/badge/react-61DAFB?style=for-the-badge&logo=react&logoColor=black" width="auto" height="25">
<img src="https://img.shields.io/badge/pwa-5A0FC8?style=for-the-badge&logo=pwa&logoColor=black" width="auto" height="25">
<img src="https://img.shields.io/badge/tailwind-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/zustand-FF9900?style=for-the-badge&logo=zustand&logoColor=white" width="auto" height="25">

Back
<br/>
<img src="https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/SPRING DATA JPA-6DB33F?style=for-the-badge&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/SPRING SECURITY-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/Spring Batch-6DB33F?style=for-the-badge&logo=Spring Batch&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/Spring Cloud Config-6DB33F?style=for-the-badge&logo=Spring Cloud Config&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/Spring Webflux-6DB33F?style=for-the-badge&logo=sSpring Webflux&logoColor=white" width="auto" height="25">

Data/AI
<br/>
<img src="https://img.shields.io/badge/python-3776AB?style=for-the-badge&logo=python&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/django-092E20?style=for-the-badge&logo=django&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/selenium-43B02A?style=for-the-badge&logo=selenium&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/fasttext-DC382D?style=for-the-badge&logo=fasttext&logoColor=white" width="auto" height="25">

Database
<br/>
<img src="https://img.shields.io/badge/mysql-4479A1?style=for-the-badge&logo=mysql&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/mariadb-003545?style=for-the-badge&logo=mariadb&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/amazons3-569A31?style=for-the-badge&logo=amazons3&logoColor=white" width="auto" height="25">

Environment
<br/>
<img src="https://img.shields.io/badge/nginx-009639?style=for-the-badge&logo=nginx&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white" width="auto" height="25">

Cooperation
<br/>
<img src="https://img.shields.io/badge/gitlab-FC6D26?style=for-the-badge&logo=gitlab&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/jira-0052CC?style=for-the-badge&logo=jira&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/notion-000000?style=for-the-badge&logo=notion&logoColor=white" width="auto" height="25">

<br/>

## 🌐 Setting

[포팅 메뉴얼 바로가기](https://github.com/yee950419/findear/tree/master/exec)

## 🎨 아키텍처

<img width="1000" alt="image" src="https://github.com/yee950419/baekjoon/assets/65946607/44ab6870-063b-43eb-8b7d-8a6a86e04019">


## 💡 주요 기능

### 1. 소셜 로그인

- 네이버 소셜 로그인 연동으로 본인인증이 된 회원이라면 별도의 절차 없이 빠르게 가입

<img src="https://github.com/yee950419/baekjoon/assets/65946607/28c53af2-77b7-41bd-b370-94a2156223be" width="300" height="500">

### 2. 시설 관리자 등록

- 카카오맵 키워드 검색을 통해 본인의 시설을 검색하고 편리하게 관리자 등록  
  <img src="https://github.com/yee950419/baekjoon/assets/65946607/bbbb589b-6816-45f1-8df1-db98dfb4850d" width="300" height="500">
  <img src="https://github.com/yee950419/baekjoon/assets/65946607/e0e9adb2-d751-44f1-8aa3-66110e2fec2b" width="300" height="500">

### 3. 습득물 통합 조회

- Lost112 공공 API를 활용한 습득물 통합 조회 서비스 제공
- 물건 종류, 키워드, 기간으로 필터 검색
- 무한 스크롤 연동
  
  <img src="https://github.com/yee950419/baekjoon/assets/65946607/f224e142-85dd-4863-8bc7-27cb64382bad" width="300" height="500">

### 4. AI 기반 습득물 등록

- 시설 관리자는 사진과 습득물명만 입력하여 간편 등록
- AI가 이미지와 습득물명으로 분석하여 카테고리, 색상, 텍스트 설명의 내용을 추가해드립니다.

<img src="https://github.com/yee950419/baekjoon/assets/65946607/e067da1a-33fd-49aa-9fa4-421d8c00b106" width="300" height="500">
<img src="https://github.com/yee950419/baekjoon/assets/65946607/a9f0edc6-f3f5-4953-874e-5cc275a5f1ea" width="300" height="500">

### 5. 습득물 관리

- 시설관리자가 등록한 습득물에 대한 관리를 해드립니다.
- 내가 등록한 습득물 조회
- 내가 인계한 습득물 관리
- 의무 보관기관 잔여일 안내

<img src="https://github.com/yee950419/baekjoon/assets/65946607/c954e2e8-7b88-4f8e-9fad-2613b0e62c48" width="300" height="500">

### 6. 분실물 등록

- 분실자는 분실물 정보를 Findear에 등록 하는 기능을 제공합니다.
- 분실물 정보는 추후 매칭에 활용됩니다.

<img src="https://github.com/yee950419/baekjoon/assets/65946607/18410fe1-c749-4afc-bc50-76555f31b616" width="300" height="500">

### 7. 유사 습득물 매칭

- 분실자가 등록한 분실물 정보와 Findear에서 보유중인 습득물 데이터를 이용한 매칭 서비스 제공  
  <img src="https://github.com/yee950419/baekjoon/assets/65946607/2e3d1cb7-c6e5-4db5-927d-ba22ee25d0d9" width="300" height="500">

### 8. 쪽지 기능

- 분실자는 매칭되거나 습득물 페이지에서 본인의 물건을 보유중이라고 생각하는 시설 관리자에게 쪽지를 보낼 수 있다.  
  <img src="https://github.com/yee950419/baekjoon/assets/65946607/7cb1b7a4-a004-4977-8e1d-719e86f37ff1" width="300" height="500">

### 9. 웹푸시, 백그라운드 알림

- PWA 웹푸시 API를 활용하여 쪽지, 매칭 등 이벤트 발생시 사용자에게 실시간으로 알려줍니다.  
  <img src="https://github.com/yee950419/baekjoon/assets/65946607/76271ba8-e5a3-4399-bb66-6da514d33526" width="300" height="500">

### 10. 인계 기능

- 시설 관리자는 습득물 인계시 파인디어 회원인지 여부를 확인하고 인계 가능
- 회원 인계시 인계자의 인증정보를 토대로 추후 발생할 수 있는 다양한 리스크 방지 가능

<img src="https://github.com/yee950419/baekjoon/assets/65946607/2cd0aa96-4470-468c-8b20-cfebba71e565" width="300" height="500">

### 11. 다크 모드

- 다크 모드 반영으로 사용자의 편의성을 생각했습니다.

<img src="https://github.com/yee950419/baekjoon/assets/65946607/96ec75bb-1f3e-4001-9764-ce0e320bb57a" width="300" height="500">
<img src="https://github.com/yee950419/baekjoon/assets/65946607/e384b309-6b07-4010-9aae-53f5cbb86cb9" width="300" height="500">

<br/>

## 📄 문서

#### 1. ERD

<img src="https://github.com/yee950419/baekjoon/assets/65946607/94569733-996e-4a7c-a110-d06dac1bc608" width="1000" height="500">

#### [2. 요구 사항 명세서](https://freckle-protocol-9a0.notion.site/36494ed19c8e440baa3f59c08f0edb0b)

#### [3. API 명세서](https://freckle-protocol-9a0.notion.site/API-d40fabfe642e46309369d9796a37fe3d?pvs=74)

<br/>
