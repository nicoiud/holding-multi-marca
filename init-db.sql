-- Tablas para el sistema de agente con memoria

-- Historial de chat por sesión (usado por n8n memoryPostgresChat)
CREATE TABLE IF NOT EXISTS agent_chat_history (
    id          SERIAL PRIMARY KEY,
    session_id  VARCHAR(255) NOT NULL,
    message     JSONB        NOT NULL,
    created_at  TIMESTAMPTZ  DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_history_session ON agent_chat_history(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_history_created ON agent_chat_history(created_at);

-- Log de memorias guardadas (espejo estructurado de Qdrant)
CREATE TABLE IF NOT EXISTS memory_log (
    id          SERIAL PRIMARY KEY,
    point_id    BIGINT       UNIQUE NOT NULL,
    session_id  VARCHAR(255) NOT NULL DEFAULT 'global',
    category    VARCHAR(100) NOT NULL DEFAULT 'dato',
    content     TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_memory_log_session   ON memory_log(session_id);
CREATE INDEX IF NOT EXISTS idx_memory_log_category  ON memory_log(category);
CREATE INDEX IF NOT EXISTS idx_memory_log_created   ON memory_log(created_at);

-- Registro de workflows creados por el agente
CREATE TABLE IF NOT EXISTS created_workflows (
    id              SERIAL PRIMARY KEY,
    n8n_workflow_id VARCHAR(100)  NOT NULL,
    workflow_name   VARCHAR(255)  NOT NULL,
    description     TEXT,
    session_id      VARCHAR(255),
    active          BOOLEAN       DEFAULT FALSE,
    created_at      TIMESTAMPTZ   DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workflows_session ON created_workflows(session_id);
