FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl espeak-ng alsa-utils pulseaudio-utils \
	&& rm -rf /var/lib/apt/lists/*
COPY --from=build /src/target/alarm-api-1.0.0.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=10 \
	CMD curl -fsS http://127.0.0.1:8080/api/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
