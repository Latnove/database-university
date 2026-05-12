## Схема БД

Главная таблица очереди:

```sql
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
```

Индекс для быстрого взятия задач:

```sql
CREATE INDEX idx_tasks_pick
ON tasks (priority DESC, scheduled_at ASC, created_at ASC, id ASC)
WHERE status = 'Ready';
```

И сам запрос для получения задачи, здесь важна блокировка строки с недожиданием разблокировки

```sql
WITH picked AS (
    SELECT id
    FROM tasks
    WHERE status = 'Ready'
      AND scheduled_at <= now()
    ORDER BY priority DESC, scheduled_at ASC, created_at ASC, id ASC
    FOR UPDATE SKIP LOCKED
    LIMIT 1
)
UPDATE tasks t
SET status = 'Running',
    started_at = now()
FROM picked
WHERE t.id = picked.id
RETURNING t.id, t.priority, t.attempts, t.payload::text;
```

## Транзакционность PostgreSQL

Продьюсер вставляет задачу и одновременно пишет фиктивное бизнес-событие в таблицу `business_events`.

## Retry-механизм

Если воркер случайно завершает задачу ошибкой, он:

1. увеличивает `attempts`;
2. возвращает задачу в `Ready`;
3. переносит `scheduled_at` в будущее;
4. использует exponential backoff.

## LISTEN / NOTIFY

Продьюсер после вставки задачи вызывает:

```sql
NOTIFY task_queue, 'new_task';
```

Воркеры слушают канал:

```sql
LISTEN task_queue;
```

Если задач нет, воркер не делает постоянный агрессивный polling, а ждёт уведомление от PostgreSQL в течение 30 секунд к примеру

## Запуск

### Запустить PostgreSQL

```bash
docker compose up -d

psql "postgresql://postgres:postgres@localhost:5432/queuedb" -f schema.sql
```

![alt text](images/image.png)
![alt text](images/image-1.png)

### Собрать проект

```bash
mvn package
```

### Запустить два воркера

```bash
mvn exec:java -Dexec.mainClass=com.example.queue.Worker -Dexec.args=worker-1
```

```bash
mvn exec:java -Dexec.mainClass=com.example.queue.Worker -Dexec.args=worker-2
```

### Запустить продьюсера

Например, 200 задач в секунду (по умолчанию):

```bash
mvn exec:java -Dexec.mainClass=com.example.queue.Producer
```

### Запустить мониторинг

```bash
mvn exec:java -Dexec.mainClass=com.example.queue.Monitor
```

## ДЗ

## SQL для лага очереди

```sql
SELECT
    now() - min(created_at) AS queue_lag
FROM tasks
WHERE status = 'Ready'
  AND scheduled_at <= now();
```

![alt text](images/image-5.png)

очередь растёт, потому что producer создаёт задачи быстрее, чем workers успевают их обрабатывать;
`lag` увеличивается;
`avg_wait_p100` меньше, чем `avg_wait_p0`, то есть критические задачи выполняются быстрее обычных.

## SQL для пропускной способности

За последние 10 секунд:

```sql
SELECT
    count(*) / 10.0 AS throughput_tasks_per_second
FROM tasks
WHERE status IN ('Completed', 'Failed')
  AND completed_at >= now() - interval '10 seconds';
```

![alt text](images/image-6.png)

## SQL для демонстрации приоритета

```sql
SELECT
    priority,
    count(*) AS processed,
    round(avg(extract(epoch FROM started_at - created_at))::numeric, 3) AS avg_wait_seconds,
    round(percentile_cont(0.5) WITHIN GROUP (
        ORDER BY extract(epoch FROM started_at - created_at)
    )::numeric, 3) AS p50_wait_seconds,
    round(percentile_cont(0.95) WITHIN GROUP (
        ORDER BY extract(epoch FROM started_at - created_at)
    )::numeric, 3) AS p95_wait_seconds
FROM tasks
WHERE started_at IS NOT NULL
GROUP BY priority
ORDER BY priority DESC;
```

![alt text](images/image-7.png)

## Bloat и autovacuum

Для нас в данный момент в PostgreSQL характерно частое обновление строк:

- `Ready -> Running`;
- `Running -> Completed`;
- `Running -> Ready`;
- `Ready -> Failed`.

Для таблицы настроен более агрессивный autovacuum:

```sql
ALTER TABLE tasks SET (
    autovacuum_vacuum_scale_factor = 0.01,
    autovacuum_analyze_scale_factor = 0.005,
    autovacuum_vacuum_threshold = 50,
    autovacuum_analyze_threshold = 50
);
```

![alt text](images/image-8.png)

## Демонстрация слушателя событий раз в 30 секунд

![alt text](images/image-2.png)

## Демонстрация работы

![alt text](images/image-4.png)
![alt text](images/image-3.png)
