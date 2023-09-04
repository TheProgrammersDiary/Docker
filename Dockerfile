FROM eclipse-temurin:17

WORKDIR /app/
COPY GlobalTests/target/GlobalTests*.jar /app/GlobalTests.jar

ENTRYPOINT ["java", "-jar", "/app/GlobalTests.jar"]