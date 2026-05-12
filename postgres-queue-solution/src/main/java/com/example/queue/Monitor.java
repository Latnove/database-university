package com.example.queue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class Monitor {
    public static void main(String[] args) throws Exception {
        try (Connection con = Db.connect();
             Statement st = con.createStatement()) {

            while (true) {
                String sql = """
                        SELECT
                            count(*) FILTER (WHERE status = 'Ready') AS ready,
                            count(*) FILTER (WHERE status = 'Running') AS running,
                            count(*) FILTER (WHERE status = 'Completed') AS completed,
                            count(*) FILTER (WHERE status = 'Failed') AS failed,
                            coalesce(
                                extract(epoch FROM now() - min(created_at) FILTER (WHERE status = 'Ready')),
                                0
                            ) AS lag_seconds,
                            count(*) FILTER (
                                WHERE status IN ('Completed', 'Failed')
                                  AND completed_at >= now() - interval '10 seconds'
                            ) / 10.0 AS throughput_per_sec,
                            avg(extract(epoch FROM started_at - created_at))
                                FILTER (WHERE priority = 100 AND started_at IS NOT NULL) AS avg_wait_priority_100,
                            avg(extract(epoch FROM started_at - created_at))
                                FILTER (WHERE priority = 0 AND started_at IS NOT NULL) AS avg_wait_priority_0
                        FROM tasks
                        """;

                try (ResultSet rs = st.executeQuery(sql)) {
                    rs.next();
                    System.out.printf(
                            "ready=%d running=%d completed=%d failed=%d lag=%.2fs throughput=%.2f/s avg_wait_p100=%.2fs avg_wait_p0=%.2fs%n",
                            rs.getLong("ready"),
                            rs.getLong("running"),
                            rs.getLong("completed"),
                            rs.getLong("failed"),
                            rs.getDouble("lag_seconds"),
                            rs.getDouble("throughput_per_sec"),
                            rs.getDouble("avg_wait_priority_100"),
                            rs.getDouble("avg_wait_priority_0")
                    );
                }

                Thread.sleep(1000);
            }
        }
    }
}
