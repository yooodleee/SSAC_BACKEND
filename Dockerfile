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

# non-root 유저 생성 (보안: root 권한 없이 JVM 실행)
RUN addgroup -S ssac && adduser -S ssac -G ssac

# jar 소유권을 ssac 유저로 지정
COPY --from=builder --chown=ssac:ssac /app/build/libs/*.jar app.jar

USER ssac

EXPOSE 8080

# sh -c 방식: Railway가 주입하는 PORT 환경 변수를 동적으로 참조한다.
# PORT 환경 변수 있음 → Railway 주입 포트로 실행
# PORT 환경 변수 없음 → 기본값 8080으로 실행 (로컬 개발 환경 호환)
ENTRYPOINT ["sh", "-c", \
    "java \
    -XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=30.0 \
    -XX:MaxRAMPercentage=55.0 \
    -XX:MaxMetaspaceSize=192m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -Dfile.encoding=UTF-8 \
    -Dstdout.encoding=UTF-8 \
    -Dstderr.encoding=UTF-8 \
    -Dserver.port=${PORT:-8080} \
    -jar app.jar"]
