FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 bso
COPY --from=build /build/target/bso-0.1.0-SNAPSHOT.jar /app/app.jar
USER bso
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]