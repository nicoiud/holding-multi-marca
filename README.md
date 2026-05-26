# n8n — Filtro de Noticias Tech/IA con Gemma

Workflow de n8n que recopila noticias de tecnología e inteligencia artificial desde múltiples fuentes RSS, las analiza con un agente Gemma y descarta las que son clickbait, fake news o contenido sin valor real.

## Qué hace

1. **Recopila** noticias de fuentes RSS de tech/IA cada X minutos
2. **Analiza** cada noticia con Gemma (via Ollama o Google AI Studio)
3. **Filtra** automáticamente:
   - Fake news y desinformación
   - Clickbait y títulos sensacionalistas sin sustancia
   - Noticias repetidas o sin información nueva
   - Contenido de relleno / AI slop
4. **Entrega** solo las noticias con valor real, con un resumen y score de calidad

## Stack

| Componente | Tecnología |
|---|---|
| Orquestador | n8n (self-hosted o cloud) |
| Modelo LLM | Gemma 3 (via Ollama local o Google AI Studio) |
| Fuentes | RSS feeds (TechCrunch, The Verge, Ars Technica, Hacker News, etc.) |
| Salida | Telegram / Slack / Email / Webhook |

## Fuentes RSS incluidas

- `https://techcrunch.com/feed/`
- `https://www.theverge.com/rss/index.xml`
- `https://feeds.arstechnica.com/arstechnica/technology-lab`
- `https://hnrss.org/frontpage`
- `https://www.artificialintelligence-news.com/feed/`
- `https://venturebeat.com/category/ai/feed/`

## Estructura del workflow

```
[Schedule Trigger]
       ↓
[RSS Feed Reader] × N fuentes
       ↓
[Merge & Deduplicate]
       ↓
[Gemma Agent — Análisis]
       ↓
[IF: score >= umbral]
  ├── ✅ Noticia válida → [Formateador] → [Salida (Telegram/Slack/etc)]
  └── ❌ Descartada → [Log opcional]
```

## Criterios de filtrado del agente

El agente Gemma evalúa cada noticia con este prompt base:

```
Eres un editor experto en tecnología e IA. Analiza este artículo y devuelve un JSON con:
- score: número del 1 al 10 (10 = muy valioso, 1 = basura total)
- motivo: por qué lo calificaste así (1 línea)
- resumen: 2-3 oraciones del contenido real si vale la pena

Descarta con score <= 4 si:
- Es clickbait sin sustancia ("Todo cambiará para siempre")
- Es fake news o especulación presentada como hecho
- Es contenido repetido de otro artículo
- Es publicidad disfrazada de noticia
- No aporta información nueva o accionable
```

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

## Archivos

```
.
├── README.md           ← este archivo
└── workflow.json       ← workflow exportado de n8n (próximamente)
```
