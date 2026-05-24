#!/bin/bash
set -e

echo "=== Setup: Agente n8n con Gemma + Memoria ==="

# 1. Levantar stack
echo ""
echo "[1/4] Levantando Docker Compose..."
docker compose up -d

echo ""
echo "[2/4] Esperando que Qdrant esté listo..."
until curl -sf http://localhost:6333/healthz > /dev/null 2>&1; do
  sleep 2
done
echo "     Qdrant OK"

# 3. Crear colección en Qdrant (nomic-embed-text produce vectores de 768 dims)
echo ""
echo "[3/4] Creando colección 'agent_memory' en Qdrant..."
curl -sf -X PUT http://localhost:6333/collections/agent_memory \
  -H "Content-Type: application/json" \
  -d '{
    "vectors": {
      "size": 768,
      "distance": "Cosine"
    },
    "optimizers_config": {
      "default_segment_number": 2
    },
    "replication_factor": 1
  }' | python3 -m json.tool || echo "     (colección ya existe, OK)"

# 4. Descargar modelos en Ollama
echo ""
echo "[4/4] Descargando modelos en Ollama..."
echo "     → gemma3:12b (razonamiento principal)"
ollama pull gemma3:12b

echo "     → nomic-embed-text (embeddings para memoria)"
ollama pull nomic-embed-text

echo ""
echo "=== LISTO ==="
echo ""
echo "Próximos pasos:"
echo "  1. Abrí n8n en http://localhost:5678"
echo "  2. Importá los 4 workflows desde n8n-workflows/"
echo "     Menú → Workflows → Import from file"
echo "  3. En n8n: Settings → API → Generá un API Key"
echo "     Pegá la key en el nodo 'Crear Workflow en n8n API' del workflow 04"
echo "  4. Creá una credencial PostgreSQL en n8n:"
echo "     Host: postgres | DB: n8n | User: n8n | Pass: n8n_password"
echo "     Asignala a los nodos que la piden (workflows 01 y 03)"
echo "  5. Activá los workflows 02, 03 y 04 (los webhooks)"
echo "  6. ¡Chateá con el agente en el workflow 01!"
echo ""
echo "Qdrant UI: http://localhost:6333/dashboard"
