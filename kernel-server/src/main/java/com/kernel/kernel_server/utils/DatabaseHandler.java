package com.kernel.kernel_server.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHandler {

    // Points to the database file you just pasted in the root folder!
    private static final String URL = "jdbc:sqlite:syncron.db";

    public static Connection connect() {
        try {
            // This forces Spring Boot to recognize the SQLite driver
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(URL);
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("❌ Database Connection Failed!");
            e.printStackTrace();
            return null;
        }
    }
}