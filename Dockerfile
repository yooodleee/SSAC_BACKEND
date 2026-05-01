# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Gradle 래퍼와 의존성 명세를 먼저 복사해 레이어 캐시를 활용한다.
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY gradle.properties .
COPY config config
RUN chmod +x gradlew

# 소스 없이 의존성만 미리 다운로드 (캐시 레이어 분리)
RUN ./gradlew dependencies --no-daemon -q

# 소스 복사 후 빌드 (테스트 제외)
COPY src src
RUN ./gradlew build -x test --no-daemon

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT [ \
    "java", \
    "-Dfile.encoding=UTF-8", \
    "-Dstdout.encoding=UTF-8", \
    "-Dstderr.encoding=UTF-8", \
    "-jar", "app.jar" \
]
