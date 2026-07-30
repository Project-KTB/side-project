# 무중단 DB 스키마 전환 런북

## 1. 목적

롤링 배포 중 구버전과 신버전 백엔드가 동시에 실행되는 상황에서 다음 변경을 서비스 중단 없이 적용한다.

- `PostImage.uploaderId` 추가
- `ProfileImage.uploaderId` 추가
- `RefreshToken.refreshToken` 원문 저장 방식에서 `refreshTokenHash` 해시 전용 방식으로 전환

핵심 원칙은 기존 컬럼을 바로 이름 변경하거나 제거하지 않고 **Expand → Migrate → Contract** 순서를 지키는 것이다.

```text
Expand:   신규 nullable 컬럼 추가
Migrate:  구·신 스키마를 모두 지원하는 호환 버전 배포 및 데이터 전환
Contract: 모든 Pod가 신버전이 된 후 기존 컬럼 제거
```

## 2. 현재 코드에 대한 주의

현재 최종 코드는 `refreshTokenHash`만 사용하고, 이미지 연결 시 자신의 `uploaderId`가 저장된 행만 허용한다.

따라서 기존 운영 스키마에 현재 코드를 바로 롤링 배포하면 안 된다. 먼저 구·신 컬럼을 함께 처리하는 **호환 버전**을 만들어 배포해야 한다.

## 3. 사전 준비

### 3.1 필수 확인

- 운영 DB 스냅샷 생성
- MySQL 버전과 `ALTER TABLE` 잠금 영향 확인
- 현재 컬럼과 인덱스 확인
- Kubernetes RollingUpdate 및 readiness probe 확인
- Argo CD 자동 동기화와 마이그레이션 실행 순서 확인
- 롤백할 이전 이미지 태그 확보

### 3.2 필수 Secret

```properties
REFRESH_TOKEN_HASH_SECRET=<32바이트 이상의 충분히 긴 랜덤 값>
```

- `JWT_SECRET`과 다른 값을 사용한다.
- 모든 백엔드 Pod에 동일한 값을 제공한다.
- 일반 재시작 과정에서는 값을 바꾸지 않는다.

### 3.3 권장 RollingUpdate 설정

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 0
    maxSurge: 1
```

실제 값은 클러스터 자원과 트래픽을 고려해 결정한다. readiness probe가 실패한 신규 Pod에는 트래픽이 전달되지 않아야 한다.

## 4. Release 1: 스키마 확장

기존 애플리케이션이 계속 동작하도록 기존 컬럼은 변경하지 않고 신규 nullable 컬럼만 추가한다.

> MySQL DDL은 암묵적으로 커밋될 수 있다. 운영 실행 전 스냅샷을 만들고 각 DDL을 개별적으로 검증한다.

### 4.1 이미지 소유자 컬럼

```sql
ALTER TABLE PostImage
    ADD COLUMN uploaderId BIGINT NULL;

ALTER TABLE ProfileImage
    ADD COLUMN uploaderId BIGINT NULL;

CREATE INDEX IDX_POST_IMAGE_UPLOADER_STATUS
    ON PostImage (uploaderId, status);

CREATE INDEX IDX_PROFILE_IMAGE_UPLOADER_STATUS
    ON ProfileImage (uploaderId, status);
```

### 4.2 Refresh Token 해시 컬럼

```sql
ALTER TABLE RefreshToken
    ADD COLUMN refreshTokenHash VARCHAR(64) NULL;

CREATE UNIQUE INDEX IDX_REFRESH_TOKEN_HASH
    ON RefreshToken (refreshTokenHash);
```

이 단계가 끝난 직후의 스키마:

| 컬럼 | 상태 | 사용자 |
| --- | --- | --- |
| `refreshToken` | 기존 `NOT NULL` | 구버전 |
| `refreshTokenHash` | 신규 `NULL` 허용 | 호환 버전 및 신버전 |

### 4.3 확장 검증

```sql
SHOW COLUMNS FROM PostImage LIKE 'uploaderId';
SHOW COLUMNS FROM ProfileImage LIKE 'uploaderId';
SHOW COLUMNS FROM RefreshToken LIKE 'refreshToken';
SHOW COLUMNS FROM RefreshToken LIKE 'refreshTokenHash';

SHOW INDEX FROM PostImage WHERE Key_name = 'IDX_POST_IMAGE_UPLOADER_STATUS';
SHOW INDEX FROM ProfileImage WHERE Key_name = 'IDX_PROFILE_IMAGE_UPLOADER_STATUS';
SHOW INDEX FROM RefreshToken WHERE Key_name = 'IDX_REFRESH_TOKEN_HASH';
```

## 5. Release 2: 호환 버전 배포

### 5.1 Refresh Token 호환 규칙

호환 버전은 다음 동작을 지원해야 한다.

#### 로그인

```text
refreshToken     = 원본 Refresh Token
refreshTokenHash = HMAC-SHA-256(원본 Refresh Token)
```

구버전 Pod가 여전히 `refreshToken NOT NULL`을 기대하므로 롤링 배포가 끝날 때까지 양쪽 값을 모두 기록한다.

#### 재발급

```text
1. refreshTokenHash로 조회
2. 없으면 기존 refreshToken으로 조회
3. 기존 컬럼으로 찾았으면 refreshTokenHash를 채움
4. 토큰 회전 시 두 컬럼을 함께 갱신
```

#### 로그아웃

```text
refreshTokenHash 또는 refreshToken으로 해당 행 삭제
```

### 5.2 이미지 소유권 호환 규칙

호환 버전은 신규 이미지 업로드 시 인증된 사용자 ID를 `uploaderId`에 기록한다.

롤링 중 구버전 Pod가 만든 `uploaderId=NULL` 이미지가 존재할 수 있으므로, 호환 버전에서는 다음 조건을 임시로 지원한다.

```text
uploaderId = 현재 사용자
OR uploaderId IS NULL
```

`NULL` 행은 실제 소유자를 증명할 수 없으므로 이 호환 규칙은 장기간 유지하지 않는다.

### 5.3 호환 버전 검증

- 구버전 Pod와 호환 Pod가 동시에 있을 때 로그인·재발급·로그아웃이 성공한다.
- 신규 로그인 행에 원문과 해시가 모두 저장된다.
- 신규 이미지 행에 인증된 사용자 ID가 저장된다.
- 게시글과 프로필 이미지 연결이 롤링 중에도 실패하지 않는다.
- 모든 호환 Pod의 readiness probe가 통과한다.

## 6. 데이터 전환

모든 Pod가 호환 버전으로 교체된 후 진행한다.

### 6.1 Refresh Token 전환 방법 A: 기존 로그인 유지

애플리케이션 배치가 기존 원문을 읽어 동일한 `REFRESH_TOKEN_HASH_SECRET`으로 HMAC-SHA-256 해시를 계산하고 `refreshTokenHash`를 채운다.

일반 SQL의 `SHA2()` 결과는 애플리케이션의 HMAC-SHA-256과 다르므로 대신 사용하지 않는다.

```sql
SELECT COUNT(*) AS missing_hash
FROM RefreshToken
WHERE refreshTokenHash IS NULL;
```

`missing_hash=0`이 될 때까지 안전한 배치 크기로 반복한다.

### 6.2 Refresh Token 전환 방법 B: 세션 초기화

기존 로그인 유지가 필요하지 않다면 다음 방법이 단순하다.

```sql
DELETE FROM RefreshToken;
```

서버는 계속 동작하지만 기존 사용자는 재발급 시 401을 받고 다시 로그인해야 한다. 서비스 무중단과 세션 무중단은 서로 다른 조건임을 운영 공지에 명시한다.

### 6.3 기존 원문 컬럼 nullable 변경

모든 Pod가 호환 버전이고 해시 전환이 끝난 후 실행한다.

```sql
ALTER TABLE RefreshToken
    MODIFY COLUMN refreshToken VARCHAR(512) NULL;
```

이 작업 이후 Hash-only 버전이 원문 컬럼을 기록하지 않아도 INSERT가 실패하지 않는다.

### 6.4 기존 이미지 전환 대기

모든 Pod가 `uploaderId`를 기록하는 호환 버전이 된 시점을 기록한다. 현재 `PENDING` 이미지 정리 기준이 24시간이므로 최소 24시간 후 다음을 확인한다.

```sql
SELECT COUNT(*) AS legacy_pending_post_images
FROM PostImage
WHERE status = 'PENDING'
  AND uploaderId IS NULL;

SELECT COUNT(*) AS legacy_pending_profile_images
FROM ProfileImage
WHERE status = 'PENDING'
  AND uploaderId IS NULL;
```

두 결과가 모두 `0`이어야 이미지 소유권 strict 버전으로 안전하게 전환할 수 있다.

## 7. Release 3: 신규 스키마 전용 버전

### 7.1 Refresh Token

- `refreshTokenHash`만 저장한다.
- 해시 컬럼으로만 조회하고 삭제한다.
- 기존 원문 컬럼은 사용하지 않는다.

### 7.2 이미지

- 인증된 업로더 ID를 반드시 저장한다.
- `status=PENDING AND uploaderId=현재 사용자`인 이미지만 연결한다.
- `uploaderId=NULL`인 신규 이미지 연결을 허용하지 않는다.

### 7.3 롤링 배포 검증

```bash
kubectl rollout status deployment/<백엔드-Deployment>
kubectl get pods
```

확인 항목:

- 모든 Pod가 동일한 신규 이미지 버전을 사용한다.
- 신규 Pod의 readiness probe가 통과한다.
- 로그인·재발급·로그아웃이 성공한다.
- DB에 신규 원문 Refresh Token이 저장되지 않는다.
- 본인 이미지 연결은 성공하고 타인 이미지 연결은 거부된다.

```sql
SELECT
    COUNT(*) AS total,
    SUM(refreshTokenHash IS NULL) AS missing_hash,
    SUM(refreshToken IS NOT NULL) AS raw_tokens
FROM RefreshToken;
```

예상 결과:

- `missing_hash=0`
- 신규 로그인 이후 `raw_tokens`가 증가하지 않음

## 8. 안정화 기간

Release 3 배포 직후 기존 컬럼을 제거하지 않는다.

최소 한 번 이상의 정상 배포 주기 동안 다음을 관찰한다.

- 인증 오류율
- Refresh Token 재발급 성공률
- 로그인 증가율
- 이미지 연결 실패율
- DB 오류
- Pod 재시작 횟수

문제가 있으면 기존 컬럼이 남아 있으므로 호환 버전으로 롤백할 수 있다.

## 9. Release 4: 스키마 축소

모든 Pod가 Hash-only 버전이고 구버전 롤백이 더 이상 필요하지 않을 때만 실행한다.

### 9.1 원문 데이터 제거

```sql
UPDATE RefreshToken
SET refreshToken = NULL
WHERE refreshTokenHash IS NOT NULL;
```

원문 값이 남아 있지 않은지 확인한다.

```sql
SELECT COUNT(*) AS raw_tokens
FROM RefreshToken
WHERE refreshToken IS NOT NULL;
```

### 9.2 기존 원문 컬럼 제거

안정화 기간과 백업을 다시 확인한 후 별도 작업으로 실행한다.

```sql
ALTER TABLE RefreshToken
    DROP COLUMN refreshToken;
```

`DROP COLUMN`은 되돌리기 어려우므로 신규 버전 배포와 같은 작업에 묶지 않는다.

## 10. 단계별 롤백

| 단계 | 롤백 방법 |
| --- | --- |
| Expand 직후 | 신규 nullable 컬럼과 인덱스를 유지한 채 구버전 계속 실행 |
| 호환 버전 배포 중 | 구버전 이미지로 롤백. 신규 컬럼은 제거하지 않음 |
| 데이터 전환 후 | 호환 버전 유지 또는 재배포 |
| Hash-only 배포 후 | 기존 원문 컬럼이 남아 있는 동안 호환 버전으로 롤백 가능 |
| 원문 컬럼 제거 후 | DB 복원 없이는 구버전 롤백 불가 |

## 11. 중단 조건

다음 상황에서는 다음 단계로 진행하지 않는다.

- 신규 Pod readiness 실패
- `refreshTokenHash` 누락 행이 남아 있음
- 호환 버전이 구·신 컬럼을 모두 처리하지 못함
- `uploaderId=NULL`인 `PENDING` 이미지가 남아 있음
- 로그인·재발급 오류율 증가
- DB DDL이 장시간 잠금을 유발
- 롤백 이미지 또는 DB 백업을 확인하지 못함

## 12. 최종 체크리스트

- [ ] 운영 DB 스냅샷 생성
- [ ] `REFRESH_TOKEN_HASH_SECRET` 등록 및 모든 Pod 동일 값 확인
- [ ] 신규 nullable 컬럼과 인덱스 추가
- [ ] 호환 버전 테스트 및 배포
- [ ] 모든 Pod가 호환 버전인지 확인
- [ ] Refresh Token 해시 역채우기 또는 세션 초기화
- [ ] 기존 `refreshToken` 컬럼 nullable 변경
- [ ] 기존 `uploaderId=NULL` PENDING 이미지 소진 확인
- [ ] Hash-only 및 owner-only 버전 롤링 배포
- [ ] 로그인·재발급·이미지 연결 Smoke test
- [ ] 안정화 기간 관찰
- [ ] 기존 원문 데이터 제거
- [ ] 마지막 별도 작업에서 기존 원문 컬럼 제거
