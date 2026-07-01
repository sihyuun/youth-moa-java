# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Gradle 래퍼 먼저 복사 (의존성 캐시 레이어 분리)
COPY gradlew gradlew
COPY gradle gradle
RUN chmod +x gradlew

# 의존성 다운로드 (소스 변경 없으면 캐시 재사용)
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon -q || true

# 소스 복사 + JAR 빌드 (테스트 스킵 — CI 에서 별도 실행)
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# 비루트 사용자로 실행 (보안 권장사항)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# JAR 복사
COPY --from=builder /workspace/build/libs/*.jar app.jar

# 포트 노출 (Fly.io 의 내부 포트 — fly.toml 의 internal_port 와 일치)
EXPOSE 8080

# Spring 프로파일·포트 환경변수로 주입 가능
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
