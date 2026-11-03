# syntax=docker/dockerfile:1.7

# ── Build: compila el jar y lo separa en capas ────────────────────────────────
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /workspace

# Las dependencias cambian menos que el código: se descargan en una capa propia
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Formato "extraído" (app.jar + lib/): el que necesita CDS, y por capas para la caché de Docker
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/benchify-backend-*.jar extract --layers --destination target/extracted \
    && mv target/extracted/application/benchify-backend-*.jar target/extracted/application/app.jar

# ── Runtime: solo JRE, sin herramientas de build, con usuario sin privilegios ─
FROM eclipse-temurin:21-jre-noble
LABEL org.opencontainers.image.title="benchify-backend" \
      org.opencontainers.image.description="Benchify REST API" \
      org.opencontainers.image.source="https://github.com/Maanuu77/Benchify"

RUN groupadd --system benchify && useradd --system --gid benchify --home-dir /app --shell /usr/sbin/nologin benchify
WORKDIR /app

# De la capa que menos cambia a la que más, para aprovechar la caché de Docker
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

# Pensado para instancias pequeñas (512 MB en el plan gratuito de Render): el heap deja sitio a
# metaspace, caché de código e hilos, y SerialGC es el recolector que menos memoria y CPU consume
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -Xss512k -XX:ReservedCodeCacheSize=64m -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Class Data Sharing: se arranca el contexto una vez (perfil cds, sin base de datos ni credenciales)
# y la JVM guarda las clases que carga. Cada arranque las lee ya preparadas:
# el arranque en frío baja de ~7 s a ~4,5 s en las pruebas locales (#66)
RUN java -XX:ArchiveClassesAtExit=app.jsa -Dspring.profiles.active=cds -Dspring.context.exit=onRefresh -jar app.jar

USER benchify

ENV SPRING_PROFILES_ACTIVE=prod \
    PORT=8080

EXPOSE 8080

# Si el archivo CDS no casa con la JVM (otra versión), Java arranca igual sin él
ENTRYPOINT ["java", "-XX:SharedArchiveFile=app.jsa", "-jar", "app.jar"]
