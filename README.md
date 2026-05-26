# n8n — Filtro de Noticias Tech/IA con Gemma

Workflow de n8n que recopila noticias de tecnología e inteligencia artificial desde múltiples fuentes RSS, las analiza con un agente Gemma, verifica su confiabilidad, genera un análisis de importancia y las publica simultáneamente en Telegram y en una web propia.

## Qué hace

1. **Recopila** noticias de fuentes RSS de tech/IA cada 30 minutos
2. **Analiza** cada noticia con Gemma — genera resumen, evalúa confiabilidad y produce un análisis editorial
3. **Filtra** noticias no confiables, fake news y clickbait
4. **Clasifica** en dos categorías con canales separados:
   - `IA` — modelos, investigación, papers, lanzamientos de AI
   - `Tech` — hardware, software, industria, empresas, ciberseguridad
5. **Publica** cada noticia aprobada en:
   - **Telegram** (canal IA o canal Tech según categoría)
   - **Web** (sitio estático servido por la API)
6. Cada noticia incluye un **análisis editorial**: si es importante, por qué, y a quién afecta

## Stack

| Componente | Tecnología |
|---|---|
| Orquestador | n8n (self-hosted, Docker) |
| Modelo LLM | Gemma 3 via Ollama (local) o Google AI Studio |
| Fuentes | RSS feeds (Ars Technica, HN, VentureBeat, OpenAI, DeepMind, etc.) |
| Salida A | Telegram (2 canales: IA y Tech) |
| Salida B | Web (Express + HTML/CSS, datos en `news.json`) |

## Fuentes RSS

**IA — alta confiabilidad**
- `https://www.artificialintelligence-news.com/feed/`
- `https://venturebeat.com/category/ai/feed/`
- `https://bair.berkeley.edu/blog/feed.xml`
- `https://openai.com/blog/rss/`
- `https://deepmind.google/blog/rss/`

**Tech — alta confiabilidad**
- `https://feeds.arstechnica.com/arstechnica/technology-lab`
- `https://hnrss.org/frontpage`
- `https://www.wired.com/feed/rss`

**Tech — confiabilidad media (filtro estricto)**
- `https://techcrunch.com/feed/`
- `https://www.theverge.com/rss/index.xml`

## Estructura del workflow

```
[Schedule Trigger: cada 30 min]
       ↓
[RSS Feed Reader] × 10 fuentes  ←→  paralelo
       ↓
[Merge + Deduplicate por URL]
       ↓
[Gemma Agent — análisis completo]
  → confiable, score, categoria, resumen, analisis_editorial
       ↓
[IF: confiable=true AND score >= 6]
  ├── ❌ Descartada → [Log JSON]
  └── ✅ Aprobada → [Switch por categoría]
                        ├── 🤖 IA
                        │     ├── [Telegram: Canal IA]
                        │     └── [POST /api/news  →  Web]
                        └── 💻 Tech
                              ├── [Telegram: Canal Tech]
                              └── [POST /api/news  →  Web]
```

## Análisis editorial por noticia

Cada noticia aprobada incluye un bloque generado por Gemma:

```
⚡ IMPORTANTE — Score 9/10
Por qué importa: Google acaba de lanzar Gemma 3 con capacidad multimodal
nativa, superando benchmarks de GPT-4o en tareas de razonamiento. Esto
democratiza modelos de alta calidad para uso local sin costo.
Impacto: desarrolladores independientes, empresas con restricciones de
privacidad, investigadores sin acceso a GPU cloud.
```

Este análisis aparece:
- Al final del mensaje de **Telegram**
- En la card de la **web**, desplegable bajo el resumen

## Formato de mensaje Telegram

```
🤖 [IA] Título de la noticia

📰 Fuente: VentureBeat  |  🕐 hace 12 min
⭐ Score: 9/10  ✅ Verificada

📋 Resumen:
Dos o tres oraciones del contenido real...

🔍 Análisis:
⚡ IMPORTANTE — Por qué importa: ...
Impacto: ...

🔗 Leer más: https://...
```

## Web (`/web`)

SPA minimalista con dos tabs (IA / Tech), cards por noticia y panel de análisis desplegable.

```
web/
├── index.html      ← estructura + tabs IA / Tech
├── style.css       ← diseño dark, tipografía limpia
└── app.js          ← fetch /api/news, renderizado dinámico
```

## API (`/api`)

Servidor Express liviano que recibe noticias de n8n y las sirve al frontend.

```
api/
├── server.js       ← Express: POST /api/news, GET /api/news
└── news.json       ← persistencia local (generado automáticamente)
```

### Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/news` | n8n publica una noticia nueva |
| `GET` | `/api/news` | Frontend obtiene todas las noticias |
| `GET` | `/api/news?categoria=IA` | Filtrar por categoría |

## Criterios del agente Gemma

El agente devuelve JSON estricto:

```json
{
  "confiable": true,
  "score": 8,
  "categoria": "IA",
  "motivo_descarte": null,
  "resumen": "Dos o tres oraciones del contenido real.",
  "analisis": {
    "importante": true,
    "nivel": "alto",
    "por_que": "Explicación de por qué importa esta noticia.",
    "impacto": "A quién afecta y cómo."
  }
}
```

### Prompt base

```
Eres un editor experto en tecnología e IA con criterio periodístico estricto.
Analiza el siguiente artículo y responde ÚNICAMENTE con el JSON indicado.

Reglas de confiabilidad (confiable=false si alguna aplica):
- No cita fuente primaria ni enlace oficial
- Usa lenguaje alarmista sin datos concretos
- Contradice información verificada sin evidencia
- Es especulación presentada como hecho confirmado

Score < 5 si:
- Clickbait sin sustancia
- Contenido repetido sin novedad
- Publicidad/PR disfrazado de noticia
- No aporta información nueva ni accionable

Categorización:
- "IA": modelos, benchmarks, papers, research labs, herramientas AI, AGI
- "Tech": hardware, software, empresas, ciberseguridad, regulación, cloud

Análisis editorial:
- "importante": true si el score >= 7
- "nivel": "alto" | "medio" | "bajo"
- "por_que": por qué esta noticia importa (1-2 oraciones directas)
- "impacto": a quién afecta y de qué forma (1 oración)
```

## Setup

### Prerrequisitos

- n8n ya instalado y corriendo
- Ollama con Gemma corriendo localmente **o** API key de Google AI Studio
- Node.js para correr la API web

### 1. Levantar la API web

```bash
cd api
npm install
node server.js
# corre en http://localhost:3000
```

### 2. Variables de entorno para n8n

En n8n → **Settings → Variables**, agregar:

| Variable | Valor |
|---|---|
| `OLLAMA_URL` | `http://localhost:11434` |
| `GEMMA_MODEL` | `gemma3` |
| `TELEGRAM_CANAL_IA` | chat ID del canal IA |
| `TELEGRAM_CANAL_TECH` | chat ID del canal Tech |
| `SCORE_MINIMO` | `6` |
| `MAX_NOTICIAS_POR_CICLO` | `20` |
| `API_SECRET` | clave para proteger la API |

### 3. Importar el workflow en n8n

1. n8n → **Workflows → Import from file**
2. Seleccionar `workflow.json`
3. Configurar credenciales: **Telegram Bot** + Ollama (HTTP Request ya apunta a `localhost:11434`)
4. Activar el workflow

## Archivos

```
.
├── README.md
├── workflow.json          ← importar en n8n
├── api/
│   ├── server.js          ← Express API (POST/GET /api/news)
│   ├── package.json
│   └── news.json          ← generado automáticamente
└── web/
    ├── index.html
    ├── style.css
    └── app.js
```
