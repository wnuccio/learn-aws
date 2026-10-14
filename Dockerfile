# Java 17 runtime only (no compiler): the jar is built beforehand by `mvn package`
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/learn-aws.jar learn-aws.jar

# documents the port GreeterServer listens on; it doesn't publish it
EXPOSE 8080

CMD ["java", "-cp", "learn-aws.jar", "org.example.fargate.GreeterServer"]