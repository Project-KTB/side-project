# Refresh Token 해시 전용 전환

이 문서는 점검 시간에 기존 세션을 초기화하고 스키마를 한 번에 변경하는 배포를 기준으로 한다.
기존 세션을 유지하는 무중단 전환이 필요하면 [`zero-downtime-db-migration.md`](./zero-downtime-db-migration.md)의 Expand–Migrate–Contract 절차를 따른다.

## 목표

Refresh Token 원문 대신 HMAC-SHA-256 해시만 `RefreshToken.refreshTokenHash` 컬럼에 저장한다.

이 절차에서는 기존 원문 토큰 행을 모두 제거한다. 기존 Refresh Cookie를 가진 사용자는 토큰 재발급 시 정상적인 401 응답을 받고 다시 로그인해야 한다.

## 필수 환경변수

신규 백엔드를 시작하기 전에 Refresh Token 해시 전용 Secret을 설정한다.

```bash
REFRESH_TOKEN_HASH_SECRET=<JWT_SECRET과 다른 충분히 긴 랜덤 Secret>
```

`refresh-token.hash-secret`은 `REFRESH_TOKEN_HASH_SECRET`에서 읽는 필수 설정이다. `jwt.secret` 또는 `JWT_SECRET`으로 대체되는 기본값이 없으므로, 설정이 누락되면 애플리케이션이 빠르게 실패한다.

정상 재시작 중에는 같은 값을 유지해야 한다. 다중 Secret을 지원하는 별도 전환 절차 없이 값을 변경하면 기존 Refresh Token을 모두 사용할 수 없게 된다.

## 운영 DB SQL

운영 DB 스냅샷과 현재 인덱스를 확인한 뒤, 기존 백엔드를 중지한 상태에서 실행한다.

> MySQL DDL은 암묵적으로 커밋될 수 있으므로 `START TRANSACTION`만으로 전체 작업의 원자성을 보장할 수 없다.

```sql
-- 기존 원문 Refresh Token을 모두 무효화하고 전체 사용자의 재로그인을 요구한다.
DELETE FROM RefreshToken;

-- 기존 원문 컬럼을 신규 애플리케이션이 요구하는 해시 컬럼으로 변경한다.
ALTER TABLE RefreshToken
    CHANGE COLUMN refreshToken refreshTokenHash VARCHAR(64) NOT NULL;
```

기존 unique 인덱스 이름이 `refreshToken`이면 현재 인덱스 이름을 확인한 후 변경하거나 다시 생성한다.

```sql
SHOW INDEX FROM RefreshToken;

-- 인덱스 이름이 실제로 refreshToken일 때만 실행하는 예시다.
-- ALTER TABLE RefreshToken DROP INDEX refreshToken;
-- CREATE UNIQUE INDEX refreshTokenHash ON RefreshToken (refreshTokenHash);
```

## 검증

배포 후 다음 내용을 확인한다.

```sql
SELECT COUNT(*) AS refresh_token_rows FROM RefreshToken;
SHOW COLUMNS FROM RefreshToken LIKE 'refreshTokenHash';
SHOW COLUMNS FROM RefreshToken LIKE 'refreshToken';
```

예상 결과:

- 배포 직후 `RefreshToken` 테이블이 비어 있다.
- `refreshTokenHash` 컬럼이 `VARCHAR(64)`로 존재한다.
- 기존 `refreshToken` 컬럼은 존재하지 않는다.
- 신규 로그인 및 재발급 행에는 JWT 형태의 원문이 아닌 64자리 소문자 16진수 해시가 저장된다.

## 배포 후 확인

1. 기존 Refresh Cookie로 재발급하면 401이 반환되고 Cookie가 제거되는지 확인한다.
2. 다시 로그인하면 Access Token과 Refresh Cookie가 발급되는지 확인한다.
3. 재발급 시 Access Token과 Refresh Cookie가 함께 회전하는지 확인한다.
4. 로그아웃 후 동일 Refresh Cookie를 다시 사용할 수 없는지 확인한다.
