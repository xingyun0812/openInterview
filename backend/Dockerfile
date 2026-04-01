FROM maven:3-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src

# 关键点：
# Spring Boot 打包后通常会出现两个 jar：
# - 可执行 fat jar：xxx.jar（包含 Main-Class / Start-Class）
# - 原始普通 jar：xxx-original.jar（不含可执行 manifest）
# 不能用通配符随意拷贝，否则容易把 original.jar 当成启动 jar，导致 “no main manifest attribute”.
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package \
  && JAR="$(ls -1 target/*.jar | grep -v -- '-original\\.jar$' | head -n 1)" \
  && test -n "${JAR}" \
  && cp "${JAR}" /app/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd -r -u 10001 -g root appuser
USER 10001

COPY --from=build /app/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

