# ---------- 1단계: 빌드 ----------
# build.gradle 의 toolchain 이 17 이므로 빌더도 JDK 17 로 맞춘다.
# (JDK 21 이미지를 쓰면 Gradle 이 17 toolchain 을 따로 프로비저닝하려다 실패할 수 있다.)
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /src

# 의존성 레이어를 소스와 분리해 캐시한다 — 소스만 바뀌면 이 레이어는 재사용된다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --quiet || true

COPY src ./src
# 테스트는 CI 가 이미 돌렸다. 이미지 빌드에서는 제외해 시간을 아낀다.
RUN ./gradlew --no-daemon clean bootJar -x test

# ---------- 2단계: 런타임 ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

ENV TZ=Asia/Seoul \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -Duser.timezone=Asia/Seoul"

# curl 은 compose 헬스체크가 쓴다. 슬림 이미지에는 들어있지 않아 직접 설치한다.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd -r app && useradd -r -g app app \
 && mkdir -p /app/uploads && chown -R app:app /app

USER app

# bootJar 산출물만 고른다. 같은 디렉터리의 `-plain.jar`(일반 jar)은 실행 불가라 반드시 배제해야 한다.
COPY --from=build /src/build/libs/borrow-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
