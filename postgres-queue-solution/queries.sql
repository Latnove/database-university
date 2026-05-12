SELECT
    now() - min(created_at) AS queue_lag
FROM tasks
WHERE status = 'Ready'
  AND scheduled_at <= now();

SELECT
    coalesce(extract(epoch FROM now() - min(created_at)), 0) AS queue_lag_seconds
FROM tasks
WHERE status = 'Ready'
  AND scheduled_at <= now();

SELECT
    count(*) / 10.0 AS throughput_tasks_per_second
FROM tasks
WHERE status IN ('Completed', 'Failed')
  AND completed_at >= now() - interval '10 seconds';

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

SELECT
    pg_size_pretty(pg_total_relation_size('tasks')) AS total_size,
    pg_size_pretty(pg_relation_size('tasks')) AS table_size,
    pg_size_pretty(pg_indexes_size('tasks')) AS indexes_size;

VACUUM ANALYZE tasks;
