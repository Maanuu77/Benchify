# syntax=docker/dockerfile:1.7

# ── Build: compila el jar y lo separa en capas ────────────────────────────────
FROM eclipse-temurin:24-jdk-noble AS build
WORKDIR /workspace

# Las dependencias cambian menos que el código: se descargan en una capa propia
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/benchify-backend-*.jar extract --layers --launcher --destination target/extracted

# ── Runtime: solo JRE, sin herramientas de build, con usuario sin privilegios ─
FROM eclipse-temurin:24-jre-noble
LABEL org.opencontainers.image.title="benchify-backend" \
      org.opencontainers.image.description="Benchify REST API" \
      org.opencontainers.image.source="https://github.com/Maanuu77/Benchify"

RUN groupadd --system benchify && useradd --system --gid benchify --home-dir /app --shell /usr/sbin/nologin benchify
WORKDIR /app

# De la capa que menos cambia a la que más, para aprovechar la caché de Docker
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

USER benchify

ENV SPRING_PROFILES_ACTIVE=prod \
    PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
