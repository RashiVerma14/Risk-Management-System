# Multi-stage production build for RiskManager
# Stage 1: Build JAR with Eclipse Temurin Java 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace/app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests=true

# Stage 2: Minimal hardened production JRE container
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S riskmanager && adduser -S riskmanager -G riskmanager
USER riskmanager

COPY --from=builder /workspace/app/target/*.jar /app/riskmanager.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "/app/riskmanager.jar"]
