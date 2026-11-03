#!/usr/bin/env bash
# Prepara un proyecto de Google Cloud / Firebase para Benchify. Se ejecuta una vez, con una sesión de
# gcloud con permisos de propietario (gcloud auth login). Se puede repetir: lo que ya existe se deja.
#
#   PROJECT_ID=mi-proyecto ./deploy/preparar-proyecto.sh
#
# Pide los datos de conexión de Neon (no se guardan en el historial de la terminal) y termina
# mostrando las variables que hay que definir en el repositorio de GitHub para el despliegue continuo.
set -euo pipefail

: "${PROJECT_ID:?Define PROJECT_ID (el ID del proyecto de Firebase)}"
REGION="${REGION:-europe-west1}"
GITHUB_REPO="${GITHUB_REPO:-Maanuu77/Benchify}"
STORAGE_BUCKET="${STORAGE_BUCKET:-${PROJECT_ID}.firebasestorage.app}"
RUNTIME_SA="benchify-api@${PROJECT_ID}.iam.gserviceaccount.com"
DEPLOY_SA="benchify-deployer@${PROJECT_ID}.iam.gserviceaccount.com"

gc() { gcloud --project "${PROJECT_ID}" --quiet "$@"; }
existe() { "$@" >/dev/null 2>&1; }

echo "→ APIs"
gc services enable run.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com \
  iamcredentials.googleapis.com sts.googleapis.com firebasehosting.googleapis.com cloudbuild.googleapis.com

echo "→ Registro de imágenes (Artifact Registry)"
existe gc artifacts repositories describe benchify --location "${REGION}" \
  || gc artifacts repositories create benchify --repository-format docker --location "${REGION}"

echo "→ Cuenta de servicio del backend (${RUNTIME_SA})"
existe gc iam service-accounts describe "${RUNTIME_SA}" \
  || gc iam service-accounts create benchify-api --display-name "Benchify API (Cloud Run)"
for rol in roles/secretmanager.secretAccessor roles/firebaseauth.admin; do
  gc projects add-iam-policy-binding "${PROJECT_ID}" --member "serviceAccount:${RUNTIME_SA}" --role "${rol}" \
    --condition None >/dev/null
done
gcloud storage buckets add-iam-policy-binding "gs://${STORAGE_BUCKET}" \
  --member "serviceAccount:${RUNTIME_SA}" --role roles/storage.objectAdmin >/dev/null

echo "→ Base de datos (Neon)"
echo "  En la consola de Neon: proyecto en AWS eu-central-1 (Fráncfort) con PostgreSQL 17 y la rama"
echo "  principal. Usa el endpoint directo (sin -pooler). Flyway crea PostGIS en la primera migración."
secreto() {
  local nombre="$1" valor="$2"
  if existe gc secrets describe "${nombre}"; then
    printf '%s' "${valor}" | gc secrets versions add "${nombre}" --data-file=- >/dev/null
  else
    printf '%s' "${valor}" | gc secrets create "${nombre}" --replication-policy automatic --data-file=- >/dev/null
  fi
}
read -r -p "  Host de Neon (p. ej. ep-xxx-123.eu-central-1.aws.neon.tech): " NEON_HOST
read -r -p "  Base de datos [neondb]: " NEON_DB
read -r -p "  Usuario [neondb_owner]: " NEON_USER
read -r -s -p "  Contraseña: " NEON_PASSWORD
echo
secreto benchify-db-url "jdbc:postgresql://${NEON_HOST}/${NEON_DB:-neondb}?sslmode=require"
secreto benchify-db-username "${NEON_USER:-neondb_owner}"
secreto benchify-db-password "${NEON_PASSWORD}"
unset NEON_PASSWORD

echo "→ Cuenta de servicio del despliegue continuo (${DEPLOY_SA})"
existe gc iam service-accounts describe "${DEPLOY_SA}" \
  || gc iam service-accounts create benchify-deployer --display-name "Benchify deploy (GitHub Actions)"
for rol in roles/run.admin roles/artifactregistry.writer roles/firebasehosting.admin \
           roles/serviceusage.serviceUsageConsumer; do
  gc projects add-iam-policy-binding "${PROJECT_ID}" --member "serviceAccount:${DEPLOY_SA}" --role "${rol}" \
    --condition None >/dev/null
done
# Para desplegar Cloud Run "como" la cuenta del backend
gc iam service-accounts add-iam-policy-binding "${RUNTIME_SA}" \
  --member "serviceAccount:${DEPLOY_SA}" --role roles/iam.serviceAccountUser >/dev/null

echo "→ Workload Identity Federation para ${GITHUB_REPO}"
existe gc iam workload-identity-pools describe github --location global \
  || gc iam workload-identity-pools create github --location global --display-name "GitHub Actions"
existe gc iam workload-identity-pools providers describe benchify --location global --workload-identity-pool github \
  || gc iam workload-identity-pools providers create-oidc benchify --location global --workload-identity-pool github \
       --issuer-uri "https://token.actions.githubusercontent.com" \
       --attribute-mapping "google.subject=assertion.sub,attribute.repository=assertion.repository" \
       --attribute-condition "assertion.repository == '${GITHUB_REPO}'"
NUMERO="$(gc projects describe "${PROJECT_ID}" --format 'value(projectNumber)')"
POOL="projects/${NUMERO}/locations/global/workloadIdentityPools/github"
gc iam service-accounts add-iam-policy-binding "${DEPLOY_SA}" --role roles/iam.workloadIdentityUser \
  --member "principalSet://iam.googleapis.com/${POOL}/attribute.repository/${GITHUB_REPO}" >/dev/null

cat <<EOF

✓ Proyecto preparado. Define estas variables en GitHub (Settings → Secrets and variables → Actions →
  Variables), o con gh:

gh variable set GCP_PROJECT_ID --body "${PROJECT_ID}"
gh variable set GCP_REGION --body "${REGION}"
gh variable set GCP_WIF_PROVIDER --body "${POOL}/providers/benchify"
gh variable set GCP_DEPLOY_SA --body "${DEPLOY_SA}"
gh variable set WEB_URL --body "https://${PROJECT_ID}.web.app"
gh variable set FIREBASE_STORAGE_BUCKET --body "${STORAGE_BUCKET}"
gh variable set FIREBASE_API_KEY --body "<apiKey de la app web de Firebase>"
gh variable set FIREBASE_APP_ID --body "<appId de la app web de Firebase>"

Falta en la consola de Firebase: crear la app web (de ahí salen apiKey y appId), activar
Authentication (Email/contraseña y Google) y Storage. Después, el primer despliegue: ver deploy/README.md.
EOF
