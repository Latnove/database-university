DROP TABLE IF EXISTS business_events;
DROP TABLE IF EXISTS tasks;

CREATE TABLE tasks (
    id           BIGSERIAL PRIMARY KEY,
    payload      JSONB NOT NULL,
    priority     INT NOT NULL DEFAULT 0,
    status       TEXT NOT NULL DEFAULT 'Ready'
                 CHECK (status IN ('Ready', 'Running', 'Completed', 'Failed')),
    attempts     INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    scheduled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at   TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_text   TEXT
);

CREATE TABLE business_events (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT REFERENCES tasks(id),
    event_type  TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tasks_pick
ON tasks (priority DESC, scheduled_at ASC, created_at ASC, id ASC)
WHERE status = 'Ready';

CREATE INDEX idx_tasks_completed_at
ON tasks (completed_at)
WHERE status IN ('Completed', 'Failed');

CREATE INDEX idx_tasks_ready_created_at
ON tasks (created_at)
WHERE status = 'Ready';

ALTER TABLE tasks SET (
    autovacuum_vacuum_scale_factor = 0.01,
    autovacuum_analyze_scale_factor = 0.005,
    autovacuum_vacuum_threshold = 50,
    autovacuum_analyze_threshold = 50
);
