package com.example.queue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;

public class Producer {
    private static final Random RND = new Random();

    public static void main(String[] args) throws Exception {
        int ratePerSecond = Integer.parseInt(System.getenv().getOrDefault("RATE", "200"));
        long sleepNanos = 1_000_000_000L / Math.max(1, ratePerSecond);

        String insertTaskSql = """
                INSERT INTO tasks(payload, priority)
                VALUES (?::jsonb, ?)
                RETURNING id
                """;

        String insertBusinessSql = """
                INSERT INTO business_events(task_id, event_type)
                VALUES (?, 'TASK_CREATED')
                """;

        try (Connection con = Db.connect();
             PreparedStatement insertTask = con.prepareStatement(insertTaskSql);
             PreparedStatement insertBusiness = con.prepareStatement(insertBusinessSql);
             Statement notifyStatement = con.createStatement()) {

            con.setAutoCommit(false);

            long produced = 0;
            long startedAt = System.currentTimeMillis();

            while (true) {
                boolean critical = RND.nextInt(100) < 20;
                int priority = critical ? 100 : 0;

                String payload = String.format(
                        "{\"type\":\"%s\",\"value\":%d,\"source\":\"producer-1\"}",
                        critical ? "critical" : "normal",
                        RND.nextInt(1_000_000)
                );

                try {
                    insertTask.setString(1, payload);
                    insertTask.setInt(2, priority);

                    long taskId;
                    try (ResultSet rs = insertTask.executeQuery()) {
                        rs.next();
                        taskId = rs.getLong(1);
                    }

                    insertBusiness.setLong(1, taskId);
                    insertBusiness.executeUpdate();

                    notifyStatement.execute("NOTIFY task_queue, 'new_task'");
                    con.commit();

                    produced++;
                    if (produced % ratePerSecond == 0) {
                        long elapsed = Math.max(1, System.currentTimeMillis() - startedAt);
                        System.out.printf(
                                "producer: produced=%d avg_rate=%.1f tasks/sec%n",
                                produced,
                                produced * 1000.0 / elapsed
                        );
                    }
                } catch (Exception e) {
                    con.rollback();
                    System.err.println("producer error: " + e.getMessage());
                }

                long ms = sleepNanos / 1_000_000L;
                int ns = (int) (sleepNanos % 1_000_000L);
                if (ms > 0 || ns > 0) {
                    Thread.sleep(ms, ns);
                }
            }
        }
    }
}
