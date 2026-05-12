package com.example.queue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
    private Db() {}

    public static Connection connect() throws SQLException {
        String url = System.getenv().getOrDefault(
                "DB_URL",
                "jdbc:postgresql://localhost:5432/queuedb"
        );
        String user = System.getenv().getOrDefault("DB_USER", "postgres");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        return DriverManager.getConnection(url, user, password);
    }
}
