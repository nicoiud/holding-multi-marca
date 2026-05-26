# n8n — Filtro de Noticias Tech/IA con Gemma

Workflow de n8n que recopila noticias de tecnología e inteligencia artificial desde múltiples fuentes RSS, las analiza con un agente Gemma, verifica su confiabilidad y las clasifica en categorías separadas antes de entregarlas.

## Qué hace

1. **Recopila** noticias de fuentes RSS de tech/IA cada X minutos
2. **Analiza** cada noticia con Gemma (via Ollama o Google AI Studio)
3. **Evalúa confiabilidad**:
   - Verifica si la fuente es reconocida y tiene historial confiable
   - Detecta fake news, desinformación y especulación presentada como hecho
   - Penaliza noticias sin fuente primaria citada
   - Descarta clickbait y contenido de relleno / AI slop
4. **Clasifica** las noticias válidas en categorías separadas:
   - `IA` — modelos, investigación, papers, lanzamientos de AI
   - `Tech` — hardware, software, industria, empresas, ciberseguridad
5. **Entrega** cada categoría por su propio canal de salida, con resumen y score

## Stack

| Componente | Tecnología |
|---|---|
| Orquestador | n8n (self-hosted o cloud) |
| Modelo LLM | Gemma 3 (via Ollama local o Google AI Studio) |
| Fuentes | RSS feeds (TechCrunch, The Verge, Ars Technica, Hacker News, etc.) |
| Salida | Telegram / Slack / Email / Webhook |

## Fuentes RSS incluidas

**IA (alta confiabilidad)**
- `https://www.artificialintelligence-news.com/feed/`
- `https://venturebeat.com/category/ai/feed/`
- `https://bair.berkeley.edu/blog/feed.xml` — investigación académica
- `https://openai.com/blog/rss/` — anuncios oficiales OpenAI
- `https://deepmind.google/blog/rss/` — anuncios oficiales DeepMind

**Tech general (alta confiabilidad)**
- `https://feeds.arstechnica.com/arstechnica/technology-lab`
- `https://hnrss.org/frontpage` — Hacker News
- `https://www.wired.com/feed/rss`

**Tech general (confiabilidad media — pasan por filtro estricto)**
- `https://techcrunch.com/feed/`
- `https://www.theverge.com/rss/index.xml`

## Estructura del workflow

```
[Schedule Trigger]
       ↓
[RSS Feed Reader] × N fuentes
       ↓
[Merge & Deduplicate]
       ↓
[Gemma Agent — Análisis de confiabilidad + clasificación]
       ↓
[IF: confiable == true AND score >= umbral]
  ├── ❌ No confiable / baja calidad → [Log descartados]
  └── ✅ Confiable → [Switch por categoría]
                          ├── 🤖 IA   → [Formateador IA]   → [Canal IA]
                          └── 💻 Tech → [Formateador Tech] → [Canal Tech]
```

## Criterios de filtrado y clasificación del agente

El agente Gemma evalúa cada noticia y devuelve un JSON estructurado:

```json
{
  "confiable": true,
  "score": 8,
  "categoria": "IA",
  "motivo_descarte": null,
  "resumen": "..."
}
```

### Prompt base del agente

```
Eres un editor experto en tecnología e IA. Analiza este artículo y responde SOLO con JSON:

{
  "confiable": boolean,       // true si la noticia es verificable y de fuente primaria
  "score": number,            // 1-10: calidad e importancia del contenido
  "categoria": "IA" | "Tech", // IA = modelos/investigación/papers; Tech = todo lo demás
  "motivo_descarte": string | null, // razón si confiable=false o score<=4
  "resumen": string | null    // 2-3 oraciones solo si vale la pena publicar
}

Marca confiable=false si:
- No cita fuente primaria ni enlace oficial
- Usa lenguaje alarmista sin datos concretos ("podría destruir", "el fin de X")
- Contradice información verificada sin evidencia
- Es especulación presentada como hecho confirmado

Baja el score por debajo de 5 si:
- Es clickbait sin sustancia real
- Es contenido repetido sin novedad
- Es publicidad o PR disfrazado de noticia
- No aporta información nueva o accionable
```

### Reglas de categorización

| Categoría | Incluye |
|---|---|
| `IA` | Nuevos modelos, benchmarks, papers, research labs, herramientas de AI, AGI/ASI |
| `Tech` | Hardware, software, empresas, ciberseguridad, regulación, startups, cloud |

## Setup rápido

### Prerrequisitos

- n8n instalado (Docker recomendado)
- Ollama con Gemma corriendo localmente **o** API key de Google AI Studio

### Con Ollama (local, gratis)

```bash
# Instalar modelo
ollama pull gemma3

# Correr n8n con acceso a Ollama
docker run -d \
  --name n8n \
  -p 5678:5678 \
  -v n8n_data:/home/node/.n8n \
  --add-host=host.docker.internal:host-gateway \
  n8nio/n8n
```

### Con Google AI Studio (cloud)

1. Obtener API key en [aistudio.google.com](https://aistudio.google.com)
2. Configurar la credencial `Google Gemini API` en n8n
3. Usar el nodo `Google Gemini Chat Model` en el workflow

## Importar el workflow

1. Abrir n8n → **Workflows** → **Import from file**
2. Seleccionar `workflow.json` de este repositorio
3. Configurar las credenciales del modelo
4. Configurar el nodo de salida (Telegram bot, Slack webhook, etc.)
5. Activar el workflow

## Variables de configuración

| Variable | Descripción | Default |
|---|---|---|
| `SCORE_MINIMO` | Puntaje mínimo para publicar | `6` |
| `INTERVALO_MINUTOS` | Frecuencia de recopilación | `30` |
| `MAX_NOTICIAS_POR_CICLO` | Límite de noticias a procesar por vuelta | `20` |
| `CANAL_IA` | Webhook/chat ID para noticias de IA | — |
| `CANAL_TECH` | Webhook/chat ID para noticias de Tech | — |
| `CANAL_DESCARTADAS` | Webhook/chat ID para log de descartadas (opcional) | — |

## Archivos

```
.
├── README.md           ← este archivo
└── workflow.json       ← workflow exportado de n8n (próximamente)
```
