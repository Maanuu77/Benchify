#!/usr/bin/env bash
# Crea, una vez, la identidad con la que GitHub Actions publica el frontend en Firebase Hosting sin
# claves (Workload Identity Federation). Todo es gratuito y no necesita activar la facturación.
# Se ejecuta con una sesión de gcloud del propietario del proyecto (p. ej. en Cloud Shell) y se puede
# repetir: lo que ya existe se deja.
#
#   PROJECT_ID=mi-proyecto ./deploy/preparar-proyecto.sh
set -euo pipefail

: "${PROJECT_ID:?Define PROJECT_ID (el ID del proyecto de Firebase)}"
GITHUB_REPO="${GITHUB_REPO:-Maanuu77/Benchify}"
DEPLOY_SA="benchify-deployer@${PROJECT_ID}.iam.gserviceaccount.com"

gc() { gcloud --project "${PROJECT_ID}" --quiet "$@"; }
existe() { "$@" >/dev/null 2>&1; }

echo "→ APIs"
gc services enable iam.googleapis.com iamcredentials.googleapis.com sts.googleapis.com \
  firebasehosting.googleapis.com

echo "→ Cuenta de servicio del despliegue (${DEPLOY_SA})"
existe gc iam service-accounts describe "${DEPLOY_SA}" \
  || gc iam service-accounts create benchify-deployer --display-name "Benchify deploy (GitHub Actions)"
for rol in roles/firebasehosting.admin roles/serviceusage.serviceUsageConsumer; do
  gc projects add-iam-policy-binding "${PROJECT_ID}" --member "serviceAccount:${DEPLOY_SA}" --role "${rol}" \
    --condition None >/dev/null
done

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

✓ Listo. Variables del repositorio de GitHub para el despliegue (Settings → Secrets and variables →
  Actions → Variables):

GCP_WIF_PROVIDER = ${POOL}/providers/benchify
GCP_DEPLOY_SA    = ${DEPLOY_SA}
EOF
