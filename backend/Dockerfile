FROM maven:3-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd -r -u 10001 -g root appuser
USER 10001

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

