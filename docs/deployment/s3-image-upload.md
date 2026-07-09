# S3 이미지 업로드 전환

## 목표

기존 EC2 로컬 디스크 저장 방식(`uploads/`) 대신 S3를 이미지 저장소로 사용한다.
기존 multipart 업로드 API는 유지하고, 2차 전환을 위해 presigned URL API도 제공한다.

## 운영 환경변수

EC2의 `/opt/side-project/.env`에 아래 값을 추가한다.

```properties
APP_IMAGE_UPLOAD_ENABLED=true
APP_IMAGE_STORAGE_TYPE=s3
APP_IMAGE_STORAGE_S3_BUCKET=<image-bucket-name>
APP_IMAGE_STORAGE_S3_REGION=ap-northeast-2
APP_IMAGE_STORAGE_S3_PUBLIC_URL_PREFIX=https://<image-domain-or-bucket-url>
APP_IMAGE_STORAGE_S3_PRESIGNED_URL_DURATION_SECONDS=300
```

`APP_IMAGE_STORAGE_S3_PUBLIC_URL_PREFIX`는 브라우저가 이미지를 조회할 공개 URL prefix다.
S3 버킷을 직접 공개하지 않고 CloudFront를 앞에 둔다면 CloudFront 도메인을 넣는다.

## AWS 권한

백엔드 EC2는 IAM Role로 S3에 접근하는 것을 권장한다.
서버 `.env`나 GitHub Secrets에 AWS Access Key를 직접 넣지 않는다.

필요 권한:

```json
{
  "Effect": "Allow",
  "Action": [
    "s3:PutObject",
    "s3:DeleteObject"
  ],
  "Resource": "arn:aws:s3:::<image-bucket-name>/*"
}
```

presigned URL 업로드를 사용하면 브라우저가 S3에 직접 `PUT` 요청을 보낸다.
이때 S3 버킷 CORS에서 프론트 도메인을 허용해야 한다.

예시:

```json
[
  {
    "AllowedOrigins": ["https://app.side-project.shop"],
    "AllowedMethods": ["PUT", "GET"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3000
  }
]
```

## API

### 기존 multipart 업로드

기존 프론트 코드와 호환된다. 서버가 파일을 받아 S3에 업로드한다.

```text
POST /api/images/posts
POST /api/images/profile
Content-Type: multipart/form-data
```

### presigned URL 업로드

1. 백엔드에서 S3 PUT URL을 발급받는다.

```http
POST /api/images/posts/presigned-url
Content-Type: application/json

{
  "originName": "sample.png",
  "contentType": "image/png",
  "fileSize": 12345
}
```

2. 응답의 `uploadUrl`로 브라우저가 직접 S3에 PUT 업로드한다.

```bash
curl -X PUT "$UPLOAD_URL" \
  -H "Content-Type: image/png" \
  --upload-file sample.png
```

3. 게시글 생성/수정 요청에는 응답의 `imageUrl`을 넣는다.

```json
{
  "title": "제목",
  "content": "내용",
  "imageUrls": ["<imageUrl>"]
}
```

## 배포 후 확인

```bash
curl -i https://api.side-project.shop/api/images/posts/presigned-url \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -d '{"originName":"sample.png","contentType":"image/png","fileSize":12345}'
```

응답에 `uploadUrl`, `imageUrl`, `storageKey`가 포함되면 백엔드 설정은 정상이다.
