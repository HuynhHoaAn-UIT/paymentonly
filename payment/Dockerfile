FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /src/target/qr-payment-1.0.0.jar /app/app.jar
USER 10001
EXPOSE 8084
ENTRYPOINT ["java","-jar","/app/app.jar"]
