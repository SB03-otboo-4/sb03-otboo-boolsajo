# 1단계: Build Stage
FROM amazoncorretto:17-alpine AS builder

# 작업 디렉토리 설정
WORKDIR /app

# 프로젝트 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN ./gradlew dependencies

COPY src src

# Gradle Wrapper를 사용하여 애플리케이션 빌드
RUN ./gradlew bootJar

# 2단계: Run Stage
FROM amazoncorretto:17-alpine

# 작업 디렉토리 설정
WORKDIR /app

# curl 설치 (선택 사항: 헬스체크용)
RUN apk add --no-cache curl

# 빌드된 jar 및 static 리소스 복사
COPY --from=builder /app/build/libs/*.jar app.jar
COPY --from=builder /app/src/main/resources/static /app/static

# JVM 옵션 외부 설정
ENV JVM_OPTS=""
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]