# Build the Spring Boot backend. The frontend is intentionally not bundled:
# it is maintained separately and can use the exposed local API.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /build

COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn .mvn
RUN sh ./mvnw -q -DskipTests dependency:go-offline

COPY backend/src src
RUN sh ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/target/steamdeck-app-*.jar app.jar
EXPOSE 3000
VOLUME ["/app/data"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
