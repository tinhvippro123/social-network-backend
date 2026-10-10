package com.vietblog;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class CreateDbTest {
    @Test
    public void createDatabase() {
        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "postgres";
        String password = "123456";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Connected to PostgreSQL server successfully.");
            stmt.executeUpdate("CREATE DATABASE vietblog");
            System.out.println("Database 'vietblog' created successfully.");
            
        } catch (Exception e) {
            System.out.println("Error or Database already exists: " + e.getMessage());
        }
    }
}
