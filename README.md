# 출발전

## Back-end 소개

- 대중교통 이동 꿀팁을 나누는 집단지성 커뮤니티 프로젝트입니다.
- `Spring Boot`로 REST API 서버를 구현하고, `MySQL`을 데이터베이스로 사용했습니다.
- 회원가입과 로그인부터 게시글, 댓글, 좋아요, 이미지 업로드, 프론트엔드 연동과 배포까지 `직접 구현`했습니다.
- Controller-Service-Repository 계층으로 구현했습니다.

### 개발 인원 및 기간

- 개발 기간 : 2026-05-12 ~ 2026-08-09
- 개발 인원 : 프론트엔드/백엔드 1명 (본인)

### 사용 기술 및 Tools

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- MySQL
- JWT
- QueryDSL
- AWS S3
- Docker
- GitHub Actions
- Prometheus

### Front-end

- <a href="https://github.com/100-hours-a-week/4-skykim-community-FE">Front-end GitHub</a>

### 서비스 시연

[영상](https://github.com/user-attachments/assets/bad8f46b-5d23-4b13-bf7c-b02cea2b866c)

### 폴더 구조

<details>
  <summary>폴더 구조 보기/숨기기</summary>
  <div markdown="1">

```text
├── .github
│   └── workflows
│       ├── deploy-backend.yml
│       └── rollback-backend.yml
├── docs
│   └── deployment
├── nginx
│   ├── nginx.blue.conf
│   └── nginx.green.conf
├── src
│   ├── main
│   │   ├── java/org/ktb/sideproject
│   │   │   ├── auth
│   │   │   ├── config
│   │   │   ├── controller
│   │   │   ├── dto
│   │   │   ├── entity
│   │   │   ├── error
│   │   │   ├── repository
│   │   │   ├── service
│   │   │   ├── validation
│   │   │   └── SideProjectApplication.java
│   │   └── resources
│   │       ├── application-prod.yml
│   │       └── application.yml
│   └── test
├── Dockerfile
├── build.gradle
├── docker-compose.prod.yml
├── gradlew
├── gradlew.bat
├── README.md
└── settings.gradle
```

  </div>
</details>

<br/>

## 서버 설계

### 서버 구조

| |Controller|Service|Repository|
|:---|:---|:---|:---|
|인증|AuthController|AuthService|RefreshTokenRepository|
|유저|UserController|UserService|UserRepository|
|게시글|PostController|PostService|PostRepository|
|댓글|CommentController|CommentService|CommentRepository|
|좋아요|PostLikeController|PostLikeService|PostLikeRepository|
|이미지|ImageController|ImageService|PostImageRepository, ProfileImageRepository|

### 구현 기능

#### Users

```text
- 회원가입, 회원 정보 조회·수정, 회원 탈퇴 기능 구현
- 이메일과 닉네임 중복 확인 및 입력값 유효성 검증
- BCrypt로 비밀번호를 암호화하여 저장
- JWT Access Token과 Refresh Token을 사용한 로그인, 로그아웃, 토큰 재발급 구현
- Refresh Token은 HttpOnly Cookie로 전달하고, DB에는 HMAC-SHA-256 해시로 저장
- Spring Security와 커스텀 JWT Filter를 통해 인증이 필요한 요청 처리
- 프로필 이미지는 스토리지에 저장하고 DB에 이미지 URL과 저장 키를 저장
```

#### Posts

```text
- 게시글 CRUD 기능 구현
- 제목 키워드 검색과 Cursor 기반 목록 조회 구현
- 게시글 조회수, 댓글수, 좋아요수 관리
- 작성자만 게시글을 수정·삭제할 수 있도록 권한 검증
- 게시글 이미지 업로드, 연결, 삭제 기능 구현
- 게시글 좋아요 등록 및 취소 기능 구현
```

#### Comments

```text
- 댓글 CRUD 기능 구현
- Cursor 기반 댓글 목록 조회 구현
- 작성자만 댓글을 수정·삭제할 수 있도록 권한 검증
- 댓글 등록·삭제 시 게시글의 댓글수 동기화
```

#### PostLikes

```text
- 게시글 좋아요 등록 및 취소 기능 구현
- 사용자별 게시글 중복 좋아요 방지
- 좋아요 등록·취소 시 게시글의 좋아요수 동기화
- 게시글 상세 조회 시 현재 사용자의 좋아요 여부 반환
```

<br/>

## 데이터베이스 설계

### 요구사항 분석

`유저 관리`
- 사용자는 이메일, 비밀번호, 닉네임, 생성·수정일시를 관리
- 이메일과 닉네임에 Unique 제약조건을 적용해 중복 방지
- 사용자는 프로필 이미지, 게시글, 댓글, 좋아요, Refresh Token과 관계를 설정

`게시글 관리`
- 사용자가 제목, 내용, 작성·수정일시, 조회수, 댓글수, 좋아요수를 포함하는 게시글 관리
- 게시글은 작성자를 참조하고 여러 개의 게시글 이미지, 댓글, 좋아요 정보와 관계를 설정

`댓글 관리`
- 사용자가 내용과 작성일시를 포함하는 댓글 관리
- 댓글은 작성자와 소속 게시글을 참조하도록 관계를 설정

`좋아요 관리`
- 사용자가 게시글에 좋아요를 등록하거나 취소할 수 있도록 관리
- 사용자와 게시글의 복합키를 사용해 한 사용자가 같은 게시글에 중복 좋아요를 누르지 못하도록 설정

`인증 관리`
- 사용자의 Refresh Token 해시를 저장하여 로그인 상태와 토큰 재발급을 관리
- 토큰 재발급 시 Access Token과 Refresh Token을 회전하고, 로그아웃 시 저장된 Refresh Token 제거

`이미지 관리`
- 게시글 이미지와 프로필 이미지의 원본 파일명, 저장 파일명, URL, Storage Key, 상태, 생성일시를 관리
- PENDING과 SAVED 상태를 구분하고 업로더 정보를 저장해 타인의 이미지 연결을 방지

### 모델링

`ERD`  
요구사항을 기반으로 모델링한 ERD입니다.  
<br/>

<img width="1490" height="862" alt="Image" src="https://github.com/user-attachments/assets/d0f4d269-cb2f-4083-81d8-1ad2960aff9d" />

<br/>

## 트러블 슈팅

### Refresh Token 원문 저장 문제

`Refresh Token을 DB에 원문으로 저장하면 DB가 노출될 경우 토큰이 그대로 악용될 수 있었습니다.`  
Refresh Token을 HMAC-SHA-256으로 해시해 DB에는 해시값만 저장하고, 재발급 시 Cookie로 받은 토큰을 같은 방식으로 해시해 비교하도록 변경했습니다.

### 이미지 저장소 및 소유권 문제

`로컬 디스크에 이미지를 저장하는 방식은 서버 재배포와 확장에 취약하고, 업로드된 이미지의 소유자를 검증할 필요가 있었습니다.`  
로컬과 S3 구현체를 분리한 스토리지 서비스를 구성하고, S3 Presigned URL 업로드를 지원했습니다. 또한 이미지에 uploaderId와 PENDING/SAVED 상태를 저장해 본인이 업로드한 이미지만 게시글과 프로필에 연결할 수 있도록 했습니다.
<br/>

## 프로젝트 후기

Spring Boot와 JPA를 사용해 게시판의 핵심 기능을 구현하며 Controller-Service-Repository 계층으로 역할을 분리하는 방법을 익혔습니다.  
또한 JWT 기반 인증·인가, Refresh Token 회전과 해시 저장, S3 이미지 업로드를 구현하며 기능 구현만큼 보안과 데이터 소유권 검증이 중요하다는 점을 배웠습니다.  
로컬 개발에서 그치지 않고 Docker, GitHub Actions, AWS, 모니터링 환경까지 연결하며 애플리케이션을 안정적으로 배포하고 운영하기 위해 코드와 인프라를 함께 고려해야 한다는 것을 경험했습니다.

<br/>

<p align="center">
  <strong>Community Back-end</strong><br/>
  게시글과 댓글로 서로 소통하는 커뮤니티
</p>
