# ProTube: backend (Spring Boot) + frontend (built into the jar) + FFmpeg for Studio.
# Build and run everything with `docker compose up -d --build` (see docs/DOCKER.md).

# ---- build: frontend + backend jar (Maven profile prod builds the frontend into static/) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY backend/pom.xml backend/pom.xml
RUN mvn -B -q -f backend/pom.xml -Pprod dependency:go-offline || true
COPY frontend frontend
COPY backend backend
RUN mvn -B -f backend/pom.xml -Pprod -DskipTests -Djacoco.skip=true package

# ---- test: the full backend suite with FFmpeg available (docker compose run --rm backend-tests) ----
FROM maven:3.9-eclipse-temurin-21 AS test
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*
ENV PROTUBE_STUDIO_FFMPEG=ffmpeg \
    PROTUBE_STUDIO_FFPROBE=ffprobe \
    ENV_PROTUBE_STORE_DIR=/tmp/protube-test-media
WORKDIR /src/backend
CMD ["mvn", "-B", "clean", "verify"]

# ---- app: what runs in production ----
FROM eclipse-temurin:21-jre AS app
# FFmpeg (with libx264/libvpx) converts Studio uploads to 360p/720p/1080p and extracts the thumbnails
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*
ENV PROTUBE_STUDIO_FFMPEG=ffmpeg \
    PROTUBE_STUDIO_FFPROBE=ffprobe \
    ENV_PROTUBE_STORE_DIR=/data/videos/ \
    PROTUBE_STUDIO_DIRECTORY=/data/studio
WORKDIR /app
COPY --from=build /src/backend/target/protube-back-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
