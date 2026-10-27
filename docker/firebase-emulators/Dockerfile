# Firebase Emulator Suite (Authentication + Storage) para desarrollo y pruebas e2e.
FROM node:26-bookworm-slim

# Algunos emuladores de firebase-tools se ejecutan sobre la JVM
RUN apt-get update \
    && apt-get install -y --no-install-recommends openjdk-17-jre-headless \
    && rm -rf /var/lib/apt/lists/*

ARG FIREBASE_TOOLS_VERSION=14.27.0
RUN npm install -g firebase-tools@${FIREBASE_TOOLS_VERSION} \
    && firebase setup:emulators:ui

WORKDIR /firebase
VOLUME /firebase/data

EXPOSE 4000 9099 9199

# Persiste usuarios y ficheros entre reinicios del contenedor
CMD ["firebase", "emulators:start", "--project", "demo-benchify", "--only", "auth,storage", \
     "--import", "/firebase/data", "--export-on-exit", "/firebase/data"]
