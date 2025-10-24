# 👔 OTBOO - **개인화 의상 및 아이템 추천 SaaS**

> 날씨, 취향을 고려해 사용자가 보유한 의상 조합을 추천해주고, 
OOTD 피드, 팔로우 등의 소셜 기능을 갖춘 서비스

[![codecov](https://codecov.io/gh/SB03-otboo-4/sb03-otboo-boolsajo/branch/dev/graph/badge.svg)](https://codecov.io/gh/SB03-otboo-4/sb03-otboo-boolsajo?branch=dev)

## 🔗 Notion

👉 [불4조밸리 Notion](https://www.notion.so/4-207649136c118078aae4d8edb6e58153?pvs=21)

---

## 👥 Team4 - Bool4Jo

| 이름 | GitHub | 개발 기능 | 개발 외 역할 |
| --- | --- | --- | --- |
| 강우진 | [WJKANGsw](https://github.com/SB03-otboo-4/sb03-otboo-boolsajo) | 인증관리 / 배포 설정 | AWS 담당 |
| 김현기 | [LZHTK](https://github.com/LZHTK) | 프로필 관리, 알림 관리 | Git 형상관리 |
| 이건민 | [GeonMin02](https://github.com/GeonMin02) | 의상 관리 및 추천/ 알림 관리 | 문서화 ( 회의록 / 노션 ) |
| 이채원 | [Chaewon Lee](https://github.com/Chaewon3Lee) | OOTD 피드 / 배포 설정 | DB 관리 |
| 조백선 | [bs8841](https://github.com/bs8841) | 날씨 데이터 관리 / 팔로우와 DM | 팀장 / PM |

---

## 🛠 기술 스택

| 분류 | 기술 |
| --- | --- |
| **Backend** | Spring Boot 3.5.3 |
| **Database** | PostgreSQL 17.5, H2 |
| **API 문서화** | Swagger UI |
| **협업 도구** | Discord, GitHub, Notion |
| **일정 관리** | Jira + Notion Timeline |

---

## 📌 주요 기능 요약

### 강우진

<img width="1911" height="921" alt="인증" src="https://github.com/user-attachments/assets/79aeb374-924a-4ce9-92e3-25f87114d4a1" />

- 인증 관리
  - 로그인
  - 비밀번호 초기화
  - CSRF 토큰 조회
  - 토큰 재발급
  - 소셜계정 연동
  - 자동 로그아웃
- 배포 설정
  - AWS 환경 구축
  - AWS 배포

---

### 김현기

<img width="1905" height="782" alt="알림" src="https://github.com/user-attachments/assets/6c4b46aa-c6d8-4d18-b4b6-0fe39aee2157" />

- 프로필 관리
   - 회원가입
   - 비밀번호 변경
   - 계정 잠금 상태 변경
   - 권한 수정
   - 프로필 조회
   - 계정 목록 조회
   - 프로필 업데이트
- 알림 관리
   - 알림 목록 조회
   - 알림 읽음 처리
---

### 이건민

<img width="1901" height="901" alt="의상" src="https://github.com/user-attachments/assets/cc8f3b2c-bd2c-4c26-af51-75c3bd8d7526" />

- 의상 관리 및 추천
  - 사용자 의상 등록
  - 의상 속성 정의 등록
  - 의상 목록 조회
  - 의상 속성 정의 수정
  - 의상 정보 수정
  - 의상 속성 정의 목록 조회
  - 의상 속성 정의 삭제
  - 구매 링크로 옷 정보 불러오기
  - 규칙 기반 추천
- 알림 관리
  - SSE 연결/이벤트 푸시
  - SSE 에러/재연결 처리
---

### 이채원

<img width="1892" height="906" alt="피드" src="https://github.com/user-attachments/assets/7fabdc61-3cbc-4045-a71d-e70565e806db" />

- OOTD 피드
  - OOTD 피드 등록
  - 피드 댓글 등록
  - 피드 좋아요 등록
  - OOTD 피드 목록 조회
  - OOTD 피드 댓글 조회
  - OOTD 피드 수정
  - OOTD 피드 삭제
  - 피드 좋아요 취소
  - Elasticsearch를 활용한 고급 검색
- 배포 설정
   - CD 배포 설정

---

### 조백선

<img width="1907" height="859" alt="날씨21" src="https://github.com/user-attachments/assets/25f1f9d9-3d7e-42e6-b75b-71ec9651e79e" />

- 날씨 데이터 관리
   - 날씨 위치 정보 조회
   - 날씨 정보 조회
- 팔로우와 DM
   - 팔로우 생성
   - 팔로우 요약 정보 조회
   - 팔로잉 목록 조회
   - 팔로워 목록 조회
   - 팔로우 취소
   - DM 목록 조회

---

## 📁 프로젝트 구조

```java
src/
├─ main/
│  ├─ java/
│  │  └─ com/sprint/otboo/
│  │     ├─ common/                    [ 공통 모듈 ]
│  │     │  ├─ base/
│  │     │  │  ├─ BaseEntity
│  │     │  │  └─ BaseUpdatableEntity
│  │     │  ├─ config/
│  │     │  ├─ controller/
│  │     │  ├─ dto/
│  │     │  │  ├─ ErrorResponse
│  │     │  │  └─ CursorPageResponse<T>
│  │     │  ├─ exception/
│  │     │  ├─ storage/
│  │     │  └─ util/
│  │     │
│  │     ├─ auth/                      [ 인증/인가 ]
│  │     │  ├─ controller/
│  │     │  ├─ service/
│  │     │  ├─ dto/
│  │     │  ├─ jwt/
│  │     │  ├─ oauth/
│  │     │  └─ util/
│  │     ├─ ...

```

---

## 배포 URL

[https://otboopheonix.site/#/recommendations](https://otboopheonix.site/)

---

## 회고록

https://www.notion.so/codeit/3-28e6fd228e8d80cbbfe3d81346c0e30c?p=28e6fd228e8d80a689f8e6866f33a079&pm=s

---
