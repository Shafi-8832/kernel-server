package com.kernel.kernel_server;

import com.kernel.kernel_server.utils.DatabaseHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@RestController
public class PingController {

    @GetMapping("/api/ping")
    public String ping() {
        System.out.println("🔔 Ping received! Checking database...");

        try (Connection conn = DatabaseHandler.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM users")) {

            if (rs.next()) {
                int userCount = rs.getInt("total");
                return "SUCCESS! The Server is alive AND it found " + userCount + " users in the database!";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Server is alive, but the database connection FAILED. Check server console.";
        }

        return "Server alive, but something weird happened.";
    }
}