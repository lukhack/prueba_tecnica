#!/usr/bin/env bash
# Recorrido de extremo a extremo contra el entorno local (docker compose up).
# Uso: ./scripts/demo.sh   (lee TRIAGE_API_KEY / ASISTENTE_API_KEY desde .env si existe)
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f .env ] && set -a && . ./.env && set +a

TRIAGE="${TRIAGE_URL:-http://localhost:8081}"
ASISTENTE="${ASISTENTE_URL:-http://localhost:8082}"
KT="${TRIAGE_API_KEY:?Defina TRIAGE_API_KEY}"
KA="${ASISTENTE_API_KEY:?Defina ASISTENTE_API_KEY}"

json() { python -c 'import json,sys; print(json.dumps(json.load(sys.stdin), ensure_ascii=False, indent=2))' 2>/dev/null || cat; }
paso() { printf '\n\033[1;36m== %s ==\033[0m\n' "$*"; }
# El cuerpo JSON se envía por stdin (--data-binary @-) para que los acentos viajen siempre en UTF-8.
post() { curl -s -X "$1" "$2" -H "X-API-Key: $3" -H 'Content-Type: application/json; charset=utf-8' --data-binary @-; }

paso "RF-01 Registrar paciente E1 (esperado CRITICO, SLA 5 min)"
E1=$(post POST "$TRIAGE/triage/pacientes" "$KT" <<'EOF'
{"nombreCompleto":"Elena Ruiz","edad":58,"frecuenciaCardiaca":130,"presionSistolica":180,"presionDiastolica":110,
 "spo2":97,"temperatura":36.8,"dolor":8,"gcs":15,"quejaPrincipal":"Dolor torácico"}
EOF
)
echo "$E1" | json
ID=$(echo "$E1" | python -c 'import json,sys; print(json.load(sys.stdin)["id"])')

paso "RF-01 Validación: FC fuera de rango → 400 con campo"
post POST "$TRIAGE/triage/pacientes" "$KT" <<'EOF' | json
{"nombreCompleto":"Luis Gómez","edad":40,"frecuenciaCardiaca":300,"presionSistolica":120,"presionDiastolica":80,
 "spo2":97,"temperatura":36.5,"dolor":0,"gcs":15,"quejaPrincipal":"Mareo"}
EOF

paso "RF-02 REGISTRADO → EN_CONSULTA"
echo '{"estado":"EN_CONSULTA"}' | post PATCH "$TRIAGE/triage/pacientes/$ID/estado" "$KT" | json

paso "RF-02 EN_CONSULTA → REGISTRADO (inválida) → 409"
echo '{"estado":"REGISTRADO"}' | post PATCH "$TRIAGE/triage/pacientes/$ID/estado" "$KT" | json

paso "RN-04 Auditoría del paciente"
curl -s "$TRIAGE/triage/pacientes/$ID/auditoria" -H "X-API-Key: $KT" | json

paso "RF-05 Reporte de operación (última hora)"
DESDE=$(python -c 'import datetime;print((datetime.datetime.now(datetime.timezone.utc)-datetime.timedelta(hours=1)).strftime("%Y-%m-%dT%H:%M:%SZ"))')
HASTA=$(python -c 'import datetime;print((datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(hours=1)).strftime("%Y-%m-%dT%H:%M:%SZ"))')
curl -s "$TRIAGE/triage/reportes/operacion?desde=$DESDE&hasta=$HASTA" -H "X-API-Key: $KT" | json

paso "RF-04 B Estado de urgencias (vista reconstruida desde Kafka)"
sleep 2
curl -s "$ASISTENTE/asistente/estado-urgencias" -H "X-API-Key: $KA" | json

paso "RF-04 A Consulta RAG (requiere LLM_API_KEY válida; si no, verá el modo degradado 503)"
post POST "$ASISTENTE/asistente/consulta" "$KA" <<'EOF' | json
{"pregunta":"¿Cuáles son las medidas iniciales ante una crisis hipertensiva?"}
EOF

paso "Métricas de negocio"
curl -s "$TRIAGE/actuator/prometheus" | grep -E '^triage_(clasificaciones|escalamientos)_total'
curl -s "$ASISTENTE/actuator/prometheus" | grep -E '^asistente_consultas_total' || true
