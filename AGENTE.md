# Agente IA Local con n8n + Gemma3 + Qdrant

Sistema de agente autónomo que corre completamente en tu PC. Recibe instrucciones en lenguaje natural, razona, usa herramientas, guarda lo que aprende y puede crear sus propios workflows.

---

## Stack

| Componente | Rol | Puerto |
|---|---|---|
| **n8n** | Orquestador de workflows | 5678 |
| **Ollama + gemma3:12b** | LLM local (razonamiento) | 11434 |
| **Ollama + nomic-embed-text** | Embeddings para memoria | 11434 |
| **Qdrant** | Base de datos vectorial (memoria semántica) | 6333 |
| **PostgreSQL** | Historial de chat y log estructurado | 5432 |
| **chat-ui/index.html** | Frontend de chat (sin servidor) | — |

---

## Cómo arrancar

```bash
# 1. Levantar el stack
./setup.sh

# 2. En n8n (http://localhost:5678):
#    Workflows → Import from file → importar los 4 JSONs de n8n-workflows/
#    Settings  → API → generar API Key
#    Credentials → New → PostgreSQL (host: postgres, db/user/pass: n8n)

# 3. En cada workflow, completar los campos marcados con REEMPLAZAR_:
#    - Credencial PostgreSQL → workflows 01 y 03
#    - API Key n8n           → workflow 04, nodo "Crear Workflow en n8n API"

# 4. Activar workflows 02, 03 y 04 (los webhooks deben estar escuchando)

# 5. Abrir chat-ui/index.html en el browser
```

---

## Arquitectura general

```
[chat-ui]
    │  POST { chatInput, sessionId }
    ▼
[01 - Agente Principal]  ← orquesta todo
    │
    ├── busca contexto ──► [02 - Memoria: Buscar]  ──► Qdrant
    ├── guarda resultado ► [03 - Memoria: Guardar] ──► Qdrant + Postgres
    └── crea workflows ──► [04 - Workflow Creator] ──► n8n API
```

---

## Workflow 01 — Agente Principal

**Archivo:** `n8n-workflows/01-agente-principal.json`
**Trigger:** Chat UI (POST al webhook)

### Qué hace

Es el cerebro del sistema. Recibe el mensaje del usuario, arma el contexto de la conversación y delega a Gemma3 la decisión de qué herramientas usar y en qué orden.

### Flujo interno

```
Chat Trigger
    │
Set Variables de Sesión
    │  genera session_id y timestamp
    ▼
AI Agent (n8n LangChain)
    │
    ├── [modelo]  Gemma4 via Ollama ─── gemma3:12b, temp 0.1, ctx 8192
    ├── [memoria] Postgres Chat ──────── historial de la sesión actual
    │
    └── [tools disponibles]
         ├── search_memory  → POST /webhook/memoria-buscar
         ├── save_memory    → POST /webhook/memoria-guardar
         ├── create_workflow → POST /webhook/workflow-creator
         ├── http_request   → cualquier URL externa
         └── run_code       → ejecuta JS dentro de n8n
```

### Nodos clave

| Nodo | Tipo | Qué hace |
|---|---|---|
| Chat Trigger | `chatTrigger` | Expone el endpoint de chat |
| Set Variables | `set` | Genera session_id y timestamp |
| AI Agent | `agent` | Loop de razonamiento ReAct |
| Gemma4 via Ollama | `lmChatOllama` | LLM local, modelo gemma3:12b |
| Memoria Postgres | `memoryPostgresChat` | Historial de la sesión en DB |
| Tool - Buscar Memoria | `toolHttpRequest` | Llama al workflow 02 |
| Tool - Guardar Memoria | `toolHttpRequest` | Llama al workflow 03 |
| Tool - Crear Workflow | `toolHttpRequest` | Llama al workflow 04 |
| Tool - HTTP Request | `toolHttpRequest` | HTTP genérico a APIs externas |
| Tool - Ejecutar Código | `toolCode` | JS sandbox dentro de n8n |

### Ciclo de razonamiento (ReAct)

El AI Agent repite este loop hasta tener una respuesta final:

```
1. Recibe el mensaje + historial de Postgres + system prompt
2. Gemma3 decide: ¿necesito una tool?
   ├── SÍ → llama a la tool, recibe resultado, vuelve al paso 2
   └── NO → genera respuesta final → se la manda al usuario
```

Máximo 15 iteraciones por seguridad.

### System prompt

El agente tiene instrucciones para:
- Siempre buscar en memoria antes de actuar
- Descomponer tareas en pasos concretos
- Guardar resultados al finalizar
- Usar `create_workflow` cuando el usuario pide automatizar algo nuevo

---

## Workflow 02 — Memoria: Buscar

**Archivo:** `n8n-workflows/02-memoria-buscar.json`
**Trigger:** Webhook POST en `/webhook/memoria-buscar`
**Activar:** Sí (siempre activo)

### Qué hace

Convierte una query de texto en un vector de embeddings y busca los registros más similares semánticamente en Qdrant. Devuelve el contexto formateado listo para que el agente lo use.

### Flujo

```
Webhook (recibe: query, limit, score_threshold, session_id)
    │
Validar Input  ──  setea defaults (limit=5, threshold=0.65)
    │
HTTP → Ollama  ──  POST /api/embeddings con nomic-embed-text
    │              devuelve vector de 768 dimensiones
    │
Merge          ──  combina el vector con los parámetros originales
    │
HTTP → Qdrant  ──  POST /collections/agent_memory/points/search
    │              filtro opcional por session_id
    │              devuelve puntos ordenados por score de similitud
    │
Code Node      ──  formatea resultados:
    │              [Memoria 1 | Score: 0.91 | workflow]
    │              contenido de la memoria...
    │
Respond        ──  { found, count, context, memories[] }
```

### Por qué nomic-embed-text

Produce vectores de 768 dimensiones con muy buena representación semántica en español e inglés. Solo ocupa 274MB de VRAM, corre en paralelo con Gemma sin problema en la 3060.

### Threshold 0.65

Un score menor a 0.65 generalmente indica que la memoria no es relevante para la query. Se puede ajustar: más alto = más estricto = menos resultados pero más precisos.

---

## Workflow 03 — Memoria: Guardar

**Archivo:** `n8n-workflows/03-memoria-guardar.json`
**Trigger:** Webhook POST en `/webhook/memoria-guardar`
**Activar:** Sí (siempre activo)

### Qué hace

Toma un texto, lo convierte en embedding y lo persiste en Qdrant con metadata. También guarda un log en Postgres para consultas estructuradas.

### Flujo

```
Webhook (recibe: content, category, session_id)
    │
Preparar Datos  ──  genera point_id aleatorio y timestamp
    │
HTTP → Ollama   ──  embed del content con nomic-embed-text
    │
Merge           ──  combina embedding + metadata
    │
HTTP → Qdrant   ──  PUT /collections/agent_memory/points
    │              upsert: si ya existe el point_id, actualiza
    │
Postgres        ──  INSERT en memory_log (espejo estructurado)
    │              útil para queries SQL sobre las memorias
    │
Respond         ──  { success, point_id, category }
```

### Categorías de memoria

El agente puede guardar con estas categorías:
- `workflow` — workflows creados o modificados
- `resultado` — resultado de una tarea ejecutada
- `aprendizaje` — algo que aprendió que puede reusar
- `dato` — información factual que puede necesitar después
- `error` — errores y cómo se resolvieron (muy valioso)

---

## Workflow 04 — Workflow Creator

**Archivo:** `n8n-workflows/04-workflow-creator.json`
**Trigger:** Webhook POST en `/webhook/workflow-creator`
**Activar:** Sí (siempre activo)

### Qué hace

Dado un nombre y descripción en lenguaje natural, usa Gemma3 para generar el JSON de un workflow n8n válido y lo crea automáticamente via la API de n8n. Opcionalmente lo activa.

### Flujo

```
Webhook (recibe: description, workflow_name, activate)
    │
Preparar Request  ──  setea defaults y normaliza el campo activate
    │
Code: Build Prompt  ──  arma system prompt con:
    │                   - reglas de formato JSON n8n
    │                   - tipos de nodos disponibles
    │                   - ejemplo de estructura mínima
    │                   - instrucción de responder SOLO JSON
    │
HTTP → Ollama  ──  POST /api/chat con gemma3:12b
    │              temperature 0.1 (respuestas determinísticas)
    │              timeout 120s (puede tardar generando JSON largo)
    │
Code: Parsear y Validar  ──  limpia markdown si el LLM lo agregó igual
    │                        parsea JSON
    │                        valida campos obligatorios (name, nodes, connections)
    │                        valida que nodes no esté vacío
    │
IF: ¿JSON válido?
    │
    ├── SÍ ──► HTTP → n8n API  ──  POST /api/v1/workflows
    │              crea el workflow (siempre inactive inicialmente)
    │          │
    │          IF: ¿activar?
    │          ├── SÍ ──► PATCH /api/v1/workflows/{id}  { active: true }
    │          └── NO ──► skip
    │          │
    │          Merge Resultado  ──  { success, workflow_id, workflow_name, message }
    │
    └── NO ──► Error de Generación  ──  { success: false, error, message }

Responder  ──  200 si éxito, 422 si falló la generación
```

### Limitación actual

Si el LLM genera JSON inválido, el workflow responde con error pero no reintenta. Se puede mejorar agregando un loop de hasta 3 intentos con el error como feedback (ver sección de mejoras).

---

## Chat UI

**Archivo:** `chat-ui/index.html`

Un solo archivo HTML sin dependencias de servidor ni build. Se abre directo en el browser.

### Características

- **Conexión directa** al webhook del Chat Trigger de n8n
- **Markdown completo** en las respuestas del agente (código, listas, tablas)
- **Tema dark/light** persistido en localStorage
- **Configuración en runtime** — URL del webhook y nombre del modelo sin tocar código
- **Gestión de sesión** — cada tab es una sesión, botón para resetear
- **Dot de estado** — verde si n8n responde, rojo si está caído
- **Typing indicator** — tres puntos animados mientras el agente piensa

### Formato de comunicación

```
Envía:  POST { chatInput: "mensaje", sessionId: "ses-xxx" }
Recibe: { output: "respuesta del agente" }
         o { text: ... } o { message: ... }  (todos manejados)
```

---

## Base de datos

### Tablas Postgres

**`agent_chat_history`** — historial de conversación por sesión
```sql
session_id  VARCHAR  -- identifica la sesión de chat
message     JSONB    -- { role: 'user'|'assistant', content: '...' }
created_at  TIMESTAMPTZ
```

**`memory_log`** — espejo estructurado de Qdrant
```sql
point_id    BIGINT   -- mismo ID que en Qdrant
session_id  VARCHAR
category    VARCHAR  -- workflow | resultado | aprendizaje | dato | error
content     TEXT
created_at  TIMESTAMPTZ
```

**`created_workflows`** — registro de workflows generados
```sql
n8n_workflow_id  VARCHAR
workflow_name    VARCHAR
description      TEXT
session_id       VARCHAR
active           BOOLEAN
```

### Colección Qdrant

**`agent_memory`**
- Vector size: 768 (nomic-embed-text)
- Distance: Cosine
- Payload: `{ content, category, session_id, timestamp }`

---

## Mejoras identificadas

### 1. Retry con feedback en el Workflow Creator

**Problema actual:** si Gemma genera JSON inválido, falla sin reintentar.

**Solución:** agregar un loop de hasta 3 intentos donde el error del intento anterior se incluye en el siguiente prompt.

```
Intento 1: genera JSON
    │
    ├── válido → crear workflow
    └── inválido → Intento 2 con prompt:
          "Tu respuesta anterior tenía este error: {error}.
           Aquí estaba el JSON: {raw}.
           Corregilo y respondé solo con JSON válido."
```

---

### 2. Memoria con TTL y prioridad

**Problema actual:** todas las memorias tienen el mismo peso. Con el tiempo, Qdrant acumula ruido.

**Solución:** agregar campos de metadata en cada punto:
- `importance: 1-5` (el agente lo asigna al guardar)
- `access_count` (cuántas veces fue recuperada)
- `expires_at` (memorias temporales auto-expiran)

Y un workflow de limpieza semanal que elimina memorias con `importance < 2` y `access_count = 0` después de 30 días.

---

### 3. Memoria episódica vs semántica

**Problema actual:** todo va a la misma colección, mezclando hechos factuales con resultados de tareas.

**Solución:** dos colecciones en Qdrant:
- `agent_memory_semantic` — hechos, datos, aprendizajes reutilizables
- `agent_memory_episodic` — qué pasó en cada sesión (se puede comprimir o resumir)

El workflow de búsqueda consulta ambas y etiqueta el origen.

---

### 4. Agente con sub-agentes especializados

**Problema actual:** un solo agente hace todo, el system prompt crece y Gemma3:12b puede perder foco.

**Solución:** un agente orquestador que delega a agentes especializados:

```
Agente Orquestador (gemma3:12b)
    │
    ├── Agente Investigador  ──  busca info, hace HTTP requests, scraping
    ├── Agente Programador   ──  escribe y ejecuta código
    └── Agente Constructor   ──  crea y modifica workflows en n8n
```

Cada sub-agente tiene su propio system prompt acotado y sus propias tools. El orquestador decide cuál activar según la tarea.

---

### 5. Ejecución asíncrona con notificaciones

**Problema actual:** el chat espera bloqueado hasta que el agente termina. Para tareas largas (crear varios workflows, hacer muchas llamadas) esto puede tardar minutos.

**Solución:**
1. El webhook responde inmediatamente con `{ job_id: "xxx", status: "working" }`
2. El agente trabaja en background
3. Cuando termina, notifica via Telegram/email/webhook
4. La UI tiene un botón "Ver resultado del job {id}"

---

### 6. Panel de memorias en la UI

**Problema actual:** el usuario no sabe qué tiene guardado el agente.

**Solución:** una segunda pantalla en la chat UI que muestra:
- Las últimas N memorias guardadas (desde Postgres)
- Filtro por categoría
- Botón para eliminar una memoria específica
- Score de relevancia de las últimas búsquedas

---

### 7. Autoevaluación de calidad

**Problema actual:** el agente no sabe si hizo bien su trabajo.

**Solución:** al finalizar cada tarea, el agente se autoevalúa con un prompt simple:
```
"Del 1 al 5, ¿qué tan bien completaste la tarea '{task}'?
 ¿Qué harías diferente? Responder en JSON: { score, reasoning, improvement }"
```

Guardar esa evaluación en memoria con categoría `aprendizaje`. Con el tiempo el agente acumula metacognición sobre sus propios puntos débiles.

---

### 8. Cache de embeddings

**Problema actual:** cada búsqueda genera un nuevo embedding aunque la query sea casi igual.

**Solución:** tabla en Postgres que mapea `hash(query)` → `embedding[]`. Antes de llamar a Ollama, verificar si ya existe el embedding cacheado. Ahorra latencia y reduce carga en Ollama.

---

## Orden de implementación sugerido

| Prioridad | Mejora | Impacto | Esfuerzo |
|---|---|---|---|
| 🔴 Alta | Retry con feedback (WF Creator) | Reduce fallos en creación | Bajo |
| 🔴 Alta | Ejecución asíncrona | Desbloquea tareas largas | Medio |
| 🟡 Media | Panel de memorias en UI | Transparencia del agente | Bajo |
| 🟡 Media | Memoria con TTL y prioridad | Calidad de contexto | Medio |
| 🟡 Media | Cache de embeddings | Latencia y costo | Bajo |
| 🟢 Baja | Sub-agentes especializados | Calidad de razonamiento | Alto |
| 🟢 Baja | Autoevaluación | Mejora continua | Medio |
| 🟢 Baja | Memoria episódica vs semántica | Organización | Alto |
