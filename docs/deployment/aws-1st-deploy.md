# AWS 1차 배포 런북

대상 구조:

- `www.<domain>` -> Route53 Alias -> CloudFront -> S3 정적 CSR 프론트엔드
- `api.<domain>` -> Route53 Alias -> ALB -> EC2 Docker/Docker Compose Spring backend -> RDS MySQL
- 1차에서는 이미지 업로드를 운영 기능으로 제공하지 않는다. `APP_IMAGE_UPLOAD_ENABLED=false` 유지.


## 1차 배포 플래그 체크리스트

프론트 정적 빌드와 백엔드 런타임의 이미지 업로드 플래그는 반드시 같은 값으로 맞춘다. 1차 배포에서는 둘 다 `false`다.

| 위치 | 값 | 검증 |
| --- | --- | --- |
| Frontend static build | `IMAGE_UPLOAD_ENABLED=false` | `dist/config.js`에 `IMAGE_UPLOAD_ENABLED: false` 포함 |
| Backend `.env` | `APP_IMAGE_UPLOAD_ENABLED=false` | `POST /api/images/posts`, `POST /api/images/profile`, `GET /api/uploads/**`가 non-2xx |

둘 중 하나만 켜면 UI/서버 동작이 어긋난다. 2차 presigned URL 전환 때만 두 값을 함께 재검토한다.

## 프론트엔드 정적 빌드

```bash
cd /opt/4-skykim-community-FE
API_BASE_URL=https://api.<domain>/api npm run build:static
aws s3 sync dist/ s3://<frontend-bucket> --delete
aws cloudfront create-invalidation --distribution-id <distribution-id> --paths '/*'
```

검증:

```bash
curl -I https://www.<domain>/
curl https://www.<domain>/config.js
```

`config.js`의 `API_BASE_URL`은 `https://api.<domain>/api`여야 한다.

## 백엔드 EC2 `.env` 예시

EC2의 배포 디렉터리(`/opt/side-project` 등)에만 저장하고 Git에는 커밋하지 않는다.

```properties
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
SERVER_SERVLET_CONTEXT_PATH=/api
HOST_BACKEND_PORT=8080

SPRING_DATASOURCE_URL=jdbc:mysql://<rds-endpoint>:3306/side_project?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useSSL=true&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=<db-user>
SPRING_DATASOURCE_PASSWORD=<db-password>

JWT_SECRET=<base64-or-strong-secret>
JWT_ACCESS_TOKEN_EXP_SECONDS=300
JWT_REFRESH_TOKEN_EXP_SECONDS=1209600

APP_CORS_ALLOWED_ORIGINS=https://www.<domain>
APP_REFRESH_COOKIE_SECURE=true
APP_REFRESH_COOKIE_SAME_SITE=Lax
APP_REFRESH_COOKIE_DOMAIN=
APP_IMAGE_UPLOAD_ENABLED=false
APP_SEED_ENABLED=false

# 최초 RDS schema bootstrap 때만 update, 정상 운영/CD는 validate
JPA_DDL_AUTO=validate
```

## 최초 RDS schema bootstrap

빈 RDS에 최초 1회만 수행한다.

1. EC2 `.env`에서 `JPA_DDL_AUTO=update`, `APP_SEED_ENABLED=false`, `APP_IMAGE_UPLOAD_ENABLED=false`로 설정한다.
2. 컨테이너를 1회 기동해서 schema를 생성한다.

```bash
cd /opt/side-project
docker compose -f docker-compose.prod.yml up -d --build backend
docker compose -f docker-compose.prod.yml logs -f backend
curl -fsS http://localhost:8080/api/health
```

3. schema 생성이 끝나면 `.env`를 `JPA_DDL_AUTO=validate`로 되돌리고 재기동한다.

```bash
docker compose -f docker-compose.prod.yml up -d --build backend
curl -fsS http://localhost:8080/api/health
```

이후 일반 배포/CD에서 `JPA_DDL_AUTO=update`를 사용하지 않는다.

## 백엔드 재배포

```bash
cd /opt/side-project
git fetch --all --prune
git checkout <deploy-ref>
git reset --hard <deploy-ref>
./gradlew test
docker compose -f docker-compose.prod.yml up -d --build backend
curl -fsS http://localhost:8080/api/health
```

ALB target group health check path는 prod context-path 기준 `GET /api/health`로 맞춘다.

## 1차 이후 schema 변경 원칙

1차 최초 bootstrap 이후 운영/CD 기본값은 `JPA_DDL_AUTO=validate`다. 엔티티 변경으로 DB schema가 바뀌면 다음 중 하나를 먼저 수행한 뒤 배포한다.

1. 수동 SQL migration을 작성하고 RDS에 적용한 뒤 애플리케이션을 배포한다.
2. 이후 Flyway/Liquibase 같은 migration 도구를 도입한다.

일반 기능 배포/CD에서 `JPA_DDL_AUTO=update`를 다시 켜지 않는다. `update`는 fresh RDS 최초 schema bootstrap 전용이다.

구버전과 신버전이 동시에 실행되는 롤링 배포에서 컬럼을 변경해야 한다면 [`무중단 DB 스키마 전환 런북`](./zero-downtime-db-migration.md)의 Expand–Migrate–Contract 절차를 따른다.
