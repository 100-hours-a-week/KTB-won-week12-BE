# 애플리케이션을 빌드하는 단계다. 실행 이미지에는 JDK와 Gradle 캐시가 포함되지 않는다.
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# 의존성 설정과 Gradle Wrapper를 먼저 복사해 Docker 레이어 캐시를 활용한다.
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

# 소스가 변경되면 이 단계부터 다시 빌드한다.
COPY src ./src
RUN ./gradlew clean bootJar --no-daemon

# 실제 서버 실행에는 더 작은 JRE 이미지만 사용한다.
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# 컨테이너 내부에서 애플리케이션을 root 권한으로 실행하지 않는다.
RUN groupadd --system spring \
    && useradd --system --gid spring --no-create-home spring

COPY --from=builder --chown=spring:spring /workspace/build/libs/app.jar ./app.jar

USER spring

EXPOSE 8080

# JVM 메모리 제한과 Spring 설정은 배포 시 환경변수로 주입한다.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
