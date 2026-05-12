package com.example.queue;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.Random;

public class Worker {
    private static final Random RND = new Random();

    public static void main(String[] args) throws Exception {
        String workerName = args[0];

        try (Connection con = Db.connect();
             Statement listen = con.createStatement()) {

            con.setAutoCommit(false);
            listen.execute("LISTEN task_queue");
            con.commit();

            PGConnection pgConnection = con.unwrap(PGConnection.class);

            System.out.println(workerName + ": стартанул");

            while (true) {
                boolean processedAny = processOne(con, workerName);

                if (!processedAny) {
                    con.commit();
                    PGNotification[] notifications = pgConnection.getNotifications(30_000);
                    if (notifications != null) {
                        System.out.println(workerName + ": не получили уведовлений, слушаем дальше");
                    }
                }
            }
        }
    }

    private static boolean processOne(Connection con, String workerName) throws Exception {
        String pickSql = """
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
                RETURNING t.id, t.priority, t.attempts, t.payload::text
                """;

        try (PreparedStatement pick = con.prepareStatement(pickSql);
             ResultSet rs = pick.executeQuery()) {

            if (!rs.next()) {
                return false;
            }

            long taskId = rs.getLong("id");
            int priority = rs.getInt("priority");
            int attempts = rs.getInt("attempts");

            con.commit();

            long sleepMs = priority == 100
                    ? 30 + RND.nextInt(50)
                    : 80 + RND.nextInt(150);

            Thread.sleep(sleepMs);

            boolean failed = RND.nextInt(100) < 10;

            con.setAutoCommit(false);
            if (failed) {
                failOrRetry(con, taskId, attempts, workerName);
            } else {
                complete(con, taskId, workerName);
            }
            con.commit();

            return true;
        }
    }

    private static void complete(Connection con, long taskId, String workerName) throws Exception {
        String sql = """
                UPDATE tasks
                SET status = 'Completed',
                    completed_at = now(),
                    error_text = NULL
                WHERE id = ?
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, taskId);
            ps.executeUpdate();
        }

        System.out.printf("%s: завершил задачу =%d%n", workerName, taskId);
    }

    private static void failOrRetry(Connection con, long taskId, int attemptsBefore, String workerName) throws Exception {
        int nextAttempt = attemptsBefore + 1;

        String sql = """
                UPDATE tasks
                SET attempts = attempts + 1,
                    status = CASE
                        WHEN attempts + 1 >= max_attempts THEN 'Failed'
                        ELSE 'Ready'
                    END,
                    scheduled_at = CASE
                        WHEN attempts + 1 >= max_attempts THEN scheduled_at
                        ELSE now() + (interval '5 minutes')
                    END,
                    completed_at = CASE
                        WHEN attempts + 1 >= max_attempts THEN now()
                        ELSE NULL
                    END,
                    error_text = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "симмулировали ошибку воркером " + workerName);
            ps.setLong(2, taskId);
            ps.executeUpdate();
        }

        long backoffSeconds = Duration.ofMinutes(5).toSeconds() * (long) Math.pow(2, attemptsBefore);
        System.out.printf(
                "%s: упала задача =%d число неудач=%d попробуем через=%ds%n",
                workerName,
                taskId,
                nextAttempt,
                backoffSeconds
        );
    }
}
