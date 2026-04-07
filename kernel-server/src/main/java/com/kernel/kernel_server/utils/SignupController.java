package com.kernel.kernel_server;

import com.kernel.kernel_server.utils.DatabaseHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;

@RestController
public class SignupController {

    @PostMapping("/api/signup")
    public ResponseEntity<Map<String, String>> signup(@RequestBody Map<String, String> userData) {
        String id = userData.get("id");
        String name = userData.get("name");
        String email = userData.get("email");
        String role = userData.get("role");

        // The exact same SQL you used to have in your frontend!
        String sql = "INSERT INTO users (id, name, email, password, role, status) VALUES (?, ?, ?, ?, ?, 'PENDING')";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.setString(2, name);
            pstmt.setString(3, email);
            pstmt.setString(4, "PENDING_APPROVAL_PASS");
            pstmt.setString(5, role);

            pstmt.executeUpdate();
            System.out.println("✅ New " + role + " requested an account: " + name);

            return ResponseEntity.ok(Map.of("message", "Signup successful"));

        } catch (Exception e) {
            if (e.getMessage().contains("UNIQUE") || e.getMessage().contains("PRIMARY KEY")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Account already exists"));
            }
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Database Error"));
        }
    }
}