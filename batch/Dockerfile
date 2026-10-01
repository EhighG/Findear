# syntax=docker/dockerfile:1

# ---- 1단계: 빌드 ----
FROM gradle:8.14.5-jdk17 AS build
WORKDIR /workspace
ENV GRADLE_USER_HOME=/home/gradle/.gradle

# 의존성 레이어: 빌드 스크립트만 먼저 복사해 소스가 바뀌어도 재사용한다.
COPY build.gradle settings.gradle ./
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle dependencies --no-daemon > /dev/null

# 소스 복사 후 실행 가능한 Boot jar만 만든다 (테스트는 이미지 빌드에서 돌리지 않는다).
COPY src ./src
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle bootJar --no-daemon \
    && cp build/libs/*.jar /workspace/app.jar

# ---- 2단계: 런타임 ----
FROM eclipse-temurin:17.0.20.1_1-jre-noble
# 헬스체크용 curl (temurin noble 이미지에 포함돼 있어 따로 설치하지 않는다)

RUN groupadd --system --gid 10001 findear \
    && useradd --system --uid 10001 --gid findear --no-create-home --shell /usr/sbin/nologin findear

WORKDIR /app
COPY --from=build --chown=findear:findear /workspace/app.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50"
USER findear
EXPOSE 8082 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
