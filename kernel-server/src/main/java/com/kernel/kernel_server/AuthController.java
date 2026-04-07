package com.kernel.kernel_server;

import com.kernel.kernel_server.utils.DatabaseHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AuthController {

    @PostMapping("/api/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT * FROM users WHERE (email = ? OR id = ?) AND password = ?"
             );

             ) {

            pstmt.setString(1, email); // This is the loginId from the app
            pstmt.setString(2, email); // Checking both columns
            pstmt.setString(3, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // If the user exists, package their data into JSON
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", rs.getString("id"));
                userData.put("name", rs.getString("name"));
                userData.put("role", rs.getString("role"));
                userData.put("email", rs.getString("email"));
                userData.put("status", rs.getString("status"));


                System.out.println("✅ User logged in: " + userData.get("name"));
                return ResponseEntity.ok(userData); // Sends HTTP 200 Success
            } else {
                System.out.println("❌ Failed login attempt for: " + email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Database error"));
        }
    }

    @PostMapping("/api/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> data) {
        String email = data.get("email");
        String newPassword = data.get("newPassword");

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM users WHERE email = ? OR id = ?");
             PreparedStatement updateStmt = conn.prepareStatement("UPDATE users SET password = ? WHERE email = ? OR id = ?")) {

            // 1. Verify the user exists
            checkStmt.setString(1, email);
            checkStmt.setString(2, email);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // 2. Perform the reset
                updateStmt.setString(1, newPassword);
                updateStmt.setString(2, email);
                updateStmt.setString(3, email);
                updateStmt.executeUpdate();
                return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found."));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Database error"));
        }
    }
}