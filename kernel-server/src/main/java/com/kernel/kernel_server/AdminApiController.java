package com.kernel.kernel_server;

import com.kernel.kernel_server.utils.DatabaseHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@RestController
public class AdminApiController {

    @GetMapping("/api/admin/pending-users")
    public ResponseEntity<List<Map<String, String>>> getPendingUsers() {
        List<Map<String, String>> users = new ArrayList<>();
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement("SELECT id, name, role, email FROM users WHERE status = 'PENDING'")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(Map.of("id", rs.getString("id"), "name", rs.getString("name"), "role", rs.getString("role"), "email", rs.getString("email")));
            }
            return ResponseEntity.ok(users);
        } catch (Exception e) { return ResponseEntity.internalServerError().build(); }
    }

    @PutMapping("/api/admin/approve/{id}")
    public ResponseEntity<Map<String, String>> approveUser(@PathVariable String id) {
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET status = 'APPROVED' WHERE id = ?")) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
            return ResponseEntity.ok(Map.of("message", "User Approved!"));
        } catch (Exception e) { return ResponseEntity.internalServerError().build(); }
    }

    @GetMapping("/api/admin/all-users")
    public ResponseEntity<List<Map<String, String>>> getAllApprovedUsers() {
        List<Map<String, String>> users = new ArrayList<>();
        try (Connection conn = DatabaseHandler.connect();
             // UPDATE 1: Added 'email' to the SELECT query
             PreparedStatement pstmt = conn.prepareStatement("SELECT id, name, role, email FROM users WHERE status != 'PENDING' ORDER BY role DESC, id ASC")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // UPDATE 2: Added 'email' to the Map
                users.add(Map.of("id", rs.getString("id"), "name", rs.getString("name"), "role", rs.getString("role"), "email", rs.getString("email")));
            }
            return ResponseEntity.ok(users);
        } catch (Exception e) { return ResponseEntity.internalServerError().build(); }
    }

}