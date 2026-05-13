# Base image - Java 21 JRE (런타임만 필요)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 비특권 사용자 생성
RUN addgroup -S app && adduser -S app -G app

# JAR 파일 복사
ARG JAR_FILE=build/libs/*-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# 소유권 이전
RUN chown -R app:app /app

USER app

# 포트 노출
EXPOSE 8080

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
