# 이미지 업로더 소유권 전환

이 문서는 점검 시간 또는 이미지 쓰기 요청을 잠시 차단할 수 있는 배포를 기준으로 한다.
무중단 전환이 필요하면 [`zero-downtime-db-migration.md`](./zero-downtime-db-migration.md)의 호환 버전 배포 절차를 따른다.

## 목표

인증된 업로더 ID를 nullable `uploaderId` 컬럼에 저장하고 `(uploaderId, status)` 인덱스를 사용해, `PENDING` 상태의 `PostImage`와 `ProfileImage`를 업로더별로 격리한다.

운영 환경은 `spring.jpa.hibernate.ddl-auto=validate`를 사용하므로, 신규 컬럼과 인덱스를 검증하는 애플리케이션을 배포하기 전에 운영 DB에 SQL을 먼저 적용해야 한다.

## 운영 DB SQL

실제 스키마를 확인한 후 백엔드 배포 시간에 실행한다. 기존 이미지 데이터를 그대로 유지하고 별도의 역채우기가 필요하지 않도록 `uploaderId` 컬럼은 의도적으로 `NULL`을 허용한다.

> MySQL DDL은 트랜잭션처럼 보이더라도 암묵적으로 커밋될 수 있다. 실행 전에 운영 DB 스냅샷을 만들고, MySQL 버전에 따른 잠금 영향도를 확인한다.

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

이전 실행이 일부만 반영되었을 수 있으므로 다음 쿼리로 현재 상태를 확인하고, 없는 항목만 적용한다.

```sql
SHOW COLUMNS FROM PostImage LIKE 'uploaderId';
SHOW COLUMNS FROM ProfileImage LIKE 'uploaderId';
SHOW INDEX FROM PostImage WHERE Key_name = 'IDX_POST_IMAGE_UPLOADER_STATUS';
SHOW INDEX FROM ProfileImage WHERE Key_name = 'IDX_PROFILE_IMAGE_UPLOADER_STATUS';
```

## 점검 배포 순서

1. 이미지를 업로드하거나 연결하는 백엔드 쓰기 요청을 중단하거나 드레인한다.
2. 운영 MySQL에 위 SQL을 적용한다.
3. `JPA_DDL_AUTO=validate`를 사용하는 신규 백엔드를 시작한다.
4. 인증된 사용자가 이미지 또는 Presigned URL을 발급받을 수 있는지 확인한다.
5. 같은 사용자가 반환된 `imageUrl`을 게시글이나 프로필에 연결할 수 있는지 확인한다.
6. 다른 사용자가 해당 `imageUrl`을 연결할 수 없는지 확인한다.

## 검증

```sql
SHOW COLUMNS FROM PostImage LIKE 'uploaderId';
SHOW COLUMNS FROM ProfileImage LIKE 'uploaderId';
SHOW INDEX FROM PostImage WHERE Key_name = 'IDX_POST_IMAGE_UPLOADER_STATUS';
SHOW INDEX FROM ProfileImage WHERE Key_name = 'IDX_PROFILE_IMAGE_UPLOADER_STATUS';
```

예상 결과:

- 두 `uploaderId` 컬럼이 존재하고 `NULL`을 허용한다.
- `IDX_POST_IMAGE_UPLOADER_STATUS`가 `PostImage(uploaderId, status)`를 대상으로 한다.
- `IDX_PROFILE_IMAGE_UPLOADER_STATUS`가 `ProfileImage(uploaderId, status)`를 대상으로 한다.
- 신규 일반 업로드와 Presigned 업로드 행에는 인증된 사용자 ID가 저장된다.
- 기존 `uploaderId=NULL` 행은 소유자 범위가 적용된 신규 연결 경로에서 사용할 수 없다.

## 롤백

신규 컬럼을 모르는 구버전으로 애플리케이션을 롤백하더라도 nullable 컬럼과 인덱스를 유지하는 것이 안전하다. 데이터 손실을 피하기 위해 즉시 제거하지 않는다.

신규 버전이 더 이상 실행되지 않는다는 사실을 확인한 후, 별도의 명시적인 정리 작업에서만 컬럼과 인덱스 제거를 검토한다.
