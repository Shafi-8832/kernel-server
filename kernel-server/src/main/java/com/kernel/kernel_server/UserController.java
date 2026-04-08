package com.kernel.kernel_server;

import com.kernel.kernel_server.utils.DatabaseHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController {

    private void ensureProfileColumnsExist() {
        try (Connection conn = DatabaseHandler.connect();
             Statement stmt = conn.createStatement()) {
            String[] columns = {"bio", "contact_no", "github", "linkedin", "fb_link", "room_no", "photo_path", "nickname"};
            for (String col : columns) {
                try { stmt.execute("ALTER TABLE users ADD COLUMN " + col + " TEXT DEFAULT ''"); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    @GetMapping("/api/users/{id}/profile")
    public ResponseEntity<Map<String, String>> getProfile(@PathVariable String id) {
        ensureProfileColumnsExist();
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Map<String, String> data = new HashMap<>();
                data.put("bio", rs.getString("bio"));
                data.put("contact_no", rs.getString("contact_no"));
                data.put("github", rs.getString("github"));
                data.put("linkedin", rs.getString("linkedin"));
                data.put("fb_link", rs.getString("fb_link"));
                data.put("room_no", rs.getString("room_no"));
                data.put("photo_path", rs.getString("photo_path"));
                data.put("nickname", rs.getString("nickname"));
                return ResponseEntity.ok(data);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) { return ResponseEntity.internalServerError().build(); }
    }

    @PutMapping("/api/users/{id}/profile")
    public ResponseEntity<Map<String, String>> updateProfile(@PathVariable String id, @RequestBody Map<String, String> data) {
        ensureProfileColumnsExist();

        String sql = "UPDATE users SET bio=?, contact_no=?, github=?, linkedin=?, fb_link=?, room_no=?, photo_path=?, nickname=? WHERE id=?";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, data.getOrDefault("bio", ""));
            pstmt.setString(2, data.getOrDefault("contact_no", ""));
            pstmt.setString(3, data.getOrDefault("github", ""));
            pstmt.setString(4, data.getOrDefault("linkedin", ""));
            pstmt.setString(5, data.getOrDefault("fb_link", ""));
            pstmt.setString(6, data.getOrDefault("room_no", ""));
            pstmt.setString(7, data.getOrDefault("photo_path", ""));
            pstmt.setString(8, data.getOrDefault("nickname", ""));
            pstmt.setString(9, id);
            pstmt.executeUpdate();
            return ResponseEntity.ok(Map.of("message", "Profile updated"));
        } catch (Exception e) { return ResponseEntity.internalServerError().build(); }
    }

    @PutMapping("/api/users/{id}/password")
    public ResponseEntity<Map<String, String>> changePassword(@PathVariable String id, @RequestBody Map<String, String> data) {
        String currentPass = data.get("currentPassword");
        String newPass = data.get("newPassword");

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM users WHERE id = ? AND password = ?");
             PreparedStatement updateStmt = conn.prepareStatement("UPDATE users SET password = ? WHERE id = ?")) {

            // 1. Verify their old password ("buet123") is correct
            checkStmt.setString(1, id);
            checkStmt.setString(2, currentPass);

            if (checkStmt.executeQuery().next()) {
                // 2. Save the new password
                updateStmt.setString(1, newPass);
                updateStmt.setString(2, id);
                updateStmt.executeUpdate();
                return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "Current password incorrect"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}