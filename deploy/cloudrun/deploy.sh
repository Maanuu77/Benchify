#!/usr/bin/env bash
# Construye la imagen del backend con Cloud Build, la publica en Artifact Registry y despliega
# el servicio de Cloud Run descrito en service.yaml.
#
#   PROJECT_ID=mi-proyecto CLOUD_SQL_INSTANCE=mi-proyecto:europe-southwest1:benchify \
#   CORS_ALLOWED_ORIGINS=https://mi-proyecto.web.app ./deploy/cloudrun/deploy.sh
set -euo pipefail

: "${PROJECT_ID:?Define PROJECT_ID}"
: "${CLOUD_SQL_INSTANCE:?Define CLOUD_SQL_INSTANCE (proyecto:región:instancia)}"
: "${CORS_ALLOWED_ORIGINS:?Define CORS_ALLOWED_ORIGINS (URL de Firebase Hosting)}"
REGION="${REGION:-europe-southwest1}"
REPOSITORY="${REPOSITORY:-benchify}"
STORAGE_BUCKET="${STORAGE_BUCKET:-${PROJECT_ID}.appspot.com}"
TAG="${TAG:-$(git rev-parse --short HEAD)}"
IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPOSITORY}/benchify-backend:${TAG}"

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

echo "→ Building ${IMAGE}"
gcloud builds submit "${ROOT}/backend" --project "${PROJECT_ID}" --tag "${IMAGE}"

echo "→ Deploying benchify-api to Cloud Run (${REGION})"
export PROJECT_ID IMAGE CLOUD_SQL_INSTANCE STORAGE_BUCKET CORS_ALLOWED_ORIGINS
envsubst < "${ROOT}/deploy/cloudrun/service.yaml" > /tmp/benchify-service.yaml
gcloud run services replace /tmp/benchify-service.yaml --project "${PROJECT_ID}" --region "${REGION}"

# API pública: la autenticación la hace la aplicación con el ID token de Firebase
gcloud run services add-iam-policy-binding benchify-api --project "${PROJECT_ID}" --region "${REGION}" \
  --member allUsers --role roles/run.invoker >/dev/null

URL="$(gcloud run services describe benchify-api --project "${PROJECT_ID}" --region "${REGION}" --format 'value(status.url)')"
echo "✓ Backend desplegado en ${URL}"
echo "  Comprueba: curl ${URL}/actuator/health"
