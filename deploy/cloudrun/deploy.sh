#!/usr/bin/env bash
# Despliegue a mano del backend (el continuo lo hace .github/workflows/deploy.yml al llegar a main):
# construye la imagen con Cloud Build, la publica en Artifact Registry y aplica service.yaml.
#
#   PROJECT_ID=mi-proyecto ./deploy/cloudrun/deploy.sh
set -euo pipefail

: "${PROJECT_ID:?Define PROJECT_ID}"
REGION="${REGION:-europe-west1}"
# Dirección pública de la web: CORS, enlaces compartidos y sitemap
WEB_URL="${WEB_URL:-https://${PROJECT_ID}.web.app}"
CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-${WEB_URL}}"
STORAGE_BUCKET="${STORAGE_BUCKET:-${PROJECT_ID}.firebasestorage.app}"
TAG="${TAG:-$(git rev-parse --short HEAD)}"
IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/benchify/benchify-backend:${TAG}"

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

echo "→ Building ${IMAGE}"
gcloud builds submit "${ROOT}/backend" --project "${PROJECT_ID}" --tag "${IMAGE}"

echo "→ Deploying benchify-api to Cloud Run (${REGION})"
export PROJECT_ID IMAGE STORAGE_BUCKET CORS_ALLOWED_ORIGINS WEB_URL
envsubst < "${ROOT}/deploy/cloudrun/service.yaml" > /tmp/benchify-service.yaml
gcloud run services replace /tmp/benchify-service.yaml --project "${PROJECT_ID}" --region "${REGION}"

# API pública: la autenticación la hace la aplicación con el ID token de Firebase
gcloud run services add-iam-policy-binding benchify-api --project "${PROJECT_ID}" --region "${REGION}" \
  --member allUsers --role roles/run.invoker >/dev/null

URL="$(gcloud run services describe benchify-api --project "${PROJECT_ID}" --region "${REGION}" --format 'value(status.url)')"
echo "✓ Backend desplegado en ${URL}"
echo "  Comprueba: curl ${URL}/actuator/health"
