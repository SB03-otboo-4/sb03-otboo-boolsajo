[![codecov](https://codecov.io/gh/SB03-otboo-4/sb03-otboo-boolsajo/branch/dev/graph/badge.svg)](https://codecov.io/gh/SB03-otboo-4/sb03-otboo-boolsajo?branch=dev)


# 📚 OTBOO - **개인화 의상 및 아이템 추천 SaaS**

> 날씨, 취향을 고려해 사용자가 보유한 의상 조합을 추천해주고, 
OOTD 피드, 팔로우 등의 소셜 기능을 갖춘 서비스


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
| **일정 관리** | GitHub Issues + Notion Timeline |

---

## 📌 주요 기능 요약

### 강우진

---

### 김현기

---

### 이건민

---

### 이채원

---

### 조백선

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

---
