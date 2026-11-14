#!/usr/bin/env bash
set -euo pipefail

: "${ACR_NAME:?Defina ACR_NAME}"
: "${RG:?Defina RG (resource group)}"
: "${AKS_NAME:?Defina AKS_NAME}"
LOCATION="${LOCATION:-eastus}"
: "${TRIAGE_API_KEY:?}" "${ASISTENTE_API_KEY:?}" "${LLM_API_KEY:?}" "${DB_PASSWORD:?}"

cd "$(dirname "$0")/.."

echo "==> Infraestructura Azure (idempotente)"
az group create -n "$RG" -l "$LOCATION" -o none
az acr create -n "$ACR_NAME" -g "$RG" --sku Basic -o none 2>/dev/null || true
az aks create -n "$AKS_NAME" -g "$RG" --node-count 2 --node-vm-size Standard_B4ms \
  --attach-acr "$ACR_NAME" --enable-app-routing --generate-ssh-keys -o none 2>/dev/null || true
az aks get-credentials -n "$AKS_NAME" -g "$RG" --overwrite-existing

echo "==> Imágenes (build en ACR, sin Docker local)"
az acr build -r "$ACR_NAME" -t vidacruz/triage-service:1.0.0 -f triage-service/Dockerfile .
az acr build -r "$ACR_NAME" -t vidacruz/asistente-service:1.0.0 -f asistente-service/Dockerfile .

echo "==> Secrets (nunca se commitean)"
kubectl apply -f k8s/namespace.yaml
kubectl -n vidacruz create secret generic vidacruz-secrets \
  --from-literal=DB_PASSWORD="$DB_PASSWORD" \
  --from-literal=TRIAGE_API_KEY="$TRIAGE_API_KEY" \
  --from-literal=ASISTENTE_API_KEY="$ASISTENTE_API_KEY" \
  --from-literal=LLM_API_KEY="$LLM_API_KEY" \
  --from-literal=ALERTAS_WEBHOOK_URL="${ALERTAS_WEBHOOK_URL:-}" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "==> Manifiestos"
sed "s/REEMPLAZAR_ACR/$ACR_NAME/g" k8s/kustomization.yaml > /tmp/kustomization.yaml
cp /tmp/kustomization.yaml k8s/kustomization.yaml.deploy
trap 'rm -f k8s/kustomization.yaml.deploy' EXIT
kubectl kustomize k8s/ --load-restrictor LoadRestrictionsNone \
  | sed "s/REEMPLAZAR_ACR/$ACR_NAME/g" | kubectl apply -f -

kubectl -n vidacruz rollout status deployment/triage-service --timeout=300s
kubectl -n vidacruz rollout status deployment/asistente-service --timeout=300s

echo "==> URL pública"
kubectl -n vidacruz get ingress vidacruz -o jsonpath='http://{.status.loadBalancer.ingress[0].ip}{"\n"}'
