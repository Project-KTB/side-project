FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Gradle 설정 파일 먼저 복사
COPY gradlew settings.gradle build.gradle ./
# 이후에 빌드하여 캐싱 활용
COPY gradle ./gradle
# 쓰기 권한 주기
RUN chmod +x ./gradlew

# 소스코드 복사
COPY src ./src
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system appuser \
    && useradd --system --gid appuser --home-dir /app --shell /usr/sbin/nologin appuser

COPY --from=build /workspace/build/libs/*.jar /app/app.jar
RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
