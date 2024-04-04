# 분실물 통합 관리 플랫폼 Findear

<img src="/uploads/ebb3197c3e4fabc7980f4638fa6c4b4f/Findear_cut.jpg" width="600" height="300">

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

## 🛠️ 기술 스포

**Front**
<br/>
<img src="https://img.shields.io/badge/typescript-3178C6?style=for-the-badge&logo=typescript&logoColor=black" width="auto" height="25">
<img src="https://img.shields.io/badge/react-61DAFB?style=for-the-badge&logo=react&logoColor=black" width="auto" height="25">
<img src="https://img.shields.io/badge/pwa-5A0FC8?style=for-the-badge&logo=pwa&logoColor=black" width="auto" height="25">
<img src="https://img.shields.io/badge/tailwind-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white" width="auto" height="25">

**Back**
<br/>
<img src="https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/SPRING DATA JPA-6DB33F?style=for-the-badge&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/SPRING SECURITY-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" width="auto" height="25">

**Data/AI**
<br/>
<img src="https://img.shields.io/badge/python-3776AB?style=for-the-badge&logo=python&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/django-092E20?style=for-the-badge&logo=django&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/selenium-43B02A?style=for-the-badge&logo=selenium&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/fasttext-DC382D?style=for-the-badge&logo=fasttext&logoColor=white" width="auto" height="25">

**Database**
<br/>
<img src="https://img.shields.io/badge/mariadb-003545?style=for-the-badge&logo=mariadb&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/amazons3-569A31?style=for-the-badge&logo=amazons3&logoColor=white" width="auto" height="25">

**Environment**
<br/>
<img src="https://img.shields.io/badge/nginx-009639?style=for-the-badge&logo=nginx&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white" width="auto" height="25">

**Cooperation**
<br/>
<img src="https://img.shields.io/badge/gitlab-FC6D26?style=for-the-badge&logo=gitlab&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/jira-0052CC?style=for-the-badge&logo=jira&logoColor=white" width="auto" height="25">
<img src="https://img.shields.io/badge/notion-000000?style=for-the-badge&logo=notion&logoColor=white" width="auto" height="25">

<br/>

## 🌐 Setting

[포팅 메뉴얼 바로가기](./exec/comeet_porting_manual.md)

## 🎨 아키텍처

<img width="1000" alt="image" src="./doc/Comeet_Architecture.png
">

## 💡 주요 기능

### 1. 소셜 로그인

- 네이버 소셜 로그인 연동으로 본인인증이 된 회원이라면 별도의 절차 없이 빠르게 가입

<img src="/uploads/2f7fb4da9e61720e657d83aca3f54f27/IMG_3591.jpeg" width="300" height="500">

### 2. 시설 관리자 등록

- 카카오맵 키워드 검색을 통해 본인의 시설을 검색하고 편리하게 관리자 등록  
  <img src="/uploads/9df03e00922516cb92b3bdd653c27578/IMG_3592.png" width="300" height="500">
  <img src="/uploads/709db7f8a8ec1964a79897ecef6ca5b5/KakaoTalk_20240404_105024675.jpg" width="300" height="500">

### 3. 습득물 통합 조회

- Lost112 공공 API를 활용한 습득물 통합 조회 서비스 제공
- 물건 종류, 키워드, 기간으로 필터 검색
- 무한 스크롤 연동
  <img src="/uploads/d6c1c7c10375ba2c313680eb2e249328/KakaoTalk_20240404_105153351.jpg" width="300" height="500">

### 4. AI 기반 습득물 등록

- 시설 관리자는 사진과 습득물명만 입력하여 간편 등록
- AI가 이미지와 습득물명으로 분석하여 카테고리, 색상, 텍스트 설명의 내용을 추가해드립니다.

<img src="/uploads/602a61f185145bb94e61b74694bc9a38/IMG_3598.png" width="300" height="500">
<img src="/uploads/94a3b498c5e59fa11e100f0de72d1017/IMG_3596.png" width="300" height="500">

### 5. 습득물 관리

- 시설관리자가 등록한 습득물에 대한 관리를 해드립니다.
- 내가 등록한 습득물 조회
- 내가 인계한 습득물 관리
- 의무 보관기관 잔여일 안내

<img src="/uploads/d0668f76363caadd15eb6d8ab18ba721/IMG_3588.png" width="300" height="500">

### 6. 분실물 등록

- 분실자는 분실물 정보를 Findear에 등록 하는 기능을 제공합니다.
- 분실물 정보는 추후 매칭에 활용됩니다.

<img src="/uploads/2ae57166b29eca7295277f735aaa7fff/KakaoTalk_20240404_110112083.png" width="300" height="500">

### 7. 유사 습득물 매칭

- 분실자가 등록한 분실물 정보와 Findear에서 보유중인 습득물 데이터를 이용한 매칭 서비스 제공  
  <img src="/uploads/0133357502c2a289a10b594fce5e86e3/IMG_3586.png" width="300" height="500">

### 8. 쪽지 기능

- 분실자는 매칭되거나 습득물 페이지에서 본인의 물건을 보유중이라고 생각하는 시설 관리자에게 쪽지를 보낼 수 있다.  
  <img src="/uploads/3e1d493c547d607959eba0aaa049b4fb/IMG_3603.png" width="300" height="500">

### 9. 웹푸시, 백그라운드 알림

- PWA 웹푸시 API를 활용하여 쪽지, 매칭 등 이벤트 발생시 사용자에게 실시간으로 알려줍니다.  
  <img src="/uploads/50a1453f83408035f59da9400ea9fcba/IMG_3600.png" width="300" height="500">

### 10. 인계 기능

- 시설 관리자는 습득물 인계시 파인디어 회원인지 여부를 확인하고 인계 가능
- 회원 인계시 인계자의 인증정보를 토대로 추후 발생할 수 있는 다양한 리스크 방지 가능

<img src="/uploads/8e5e1590f23deefbbace398f0b908d55/IMG_3608.png" width="300" height="500">

### 11. 다크 모드

- 다크 모드 반영으로 사용자의 편의성을 생각했습니다.

<img src="/uploads/2a14c63c876dcd66edbea3ce734d537e/KakaoTalk_20240404_110403294_01.png" width="300" height="500">
<img src="/uploads/76efc71912ccec1de96d6193d3443bb4/KakaoTalk_20240404_110403294.png" width="300" height="500">

<br/>

## 📄 문서

#### 1. ERD

<img src="/uploads/749d890e75f25e1e749b84b8fcd28cde/findear_ERD_%EA%B5%AC%EC%A1%B0.PNG" width="1000" height="500">

#### [2. 요구 사항 명세서](https://freckle-protocol-9a0.notion.site/36494ed19c8e440baa3f59c08f0edb0b)

#### [3. API 명세서](https://freckle-protocol-9a0.notion.site/API-d40fabfe642e46309369d9796a37fe3d?pvs=74)

<br/>
