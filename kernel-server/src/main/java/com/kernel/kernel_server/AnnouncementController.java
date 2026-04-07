package com.kernel.kernel_server;

import com.kernel.kernel_server.utils.DatabaseHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AnnouncementController {

    private void ensureTableExists() {
        try (Connection conn = DatabaseHandler.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS announcements (id INTEGER PRIMARY KEY AUTOINCREMENT, course_code TEXT, message TEXT, timestamp TEXT, creator_id TEXT)");
        } catch (Exception ignored) {}
    }

    // 1. FETCH ALL FOR COURSE
    @GetMapping("/api/announcements/{courseCode}")
    public ResponseEntity<List<Map<String, String>>> getAnnouncements(@PathVariable String courseCode) {
        ensureTableExists();
        List<Map<String, String>> announcements = new ArrayList<>();
        // Fetching with a JOIN to get the Teacher's real name!
        String sql = "SELECT a.id, a.message, a.timestamp, u.name as author_name FROM announcements a LEFT JOIN users u ON a.creator_id = u.id WHERE a.course_code = ? ORDER BY a.id DESC";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseCode);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, String> ann = new HashMap<>();
                ann.put("id", rs.getString("id"));
                ann.put("message", rs.getString("message"));
                ann.put("timestamp", rs.getString("timestamp"));
                ann.put("author", rs.getString("author_name") != null ? rs.getString("author_name") : "System Auto-Post");
                announcements.add(ann);
            }
            return ResponseEntity.ok(announcements);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // 2. CREATE CUSTOM POST
    @PostMapping("/api/announcements")
    public ResponseEntity<Map<String, String>> createAnnouncement(@RequestBody Map<String, String> data) {
        ensureTableExists();
        String sql = "INSERT INTO announcements (course_code, message, timestamp, creator_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, data.get("courseCode"));
            pstmt.setString(2, data.get("message"));
            pstmt.setString(3, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            pstmt.setString(4, data.get("creatorId"));
            pstmt.executeUpdate();
            return ResponseEntity.ok(Map.of("message", "Posted successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // 3. DELETE POST
    @DeleteMapping("/api/announcements/{id}")
    public ResponseEntity<Map<String, String>> deleteAnnouncement(@PathVariable String id) {
        String sql = "DELETE FROM announcements WHERE id = ?";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 4. UPDATE POST
    @PutMapping("/api/announcements/{id}")
    public ResponseEntity<Map<String, String>> updateAnnouncement(@PathVariable String id, @RequestBody Map<String, String> data) {
        String sql = "UPDATE announcements SET message = ? WHERE id = ?";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, data.get("message"));
            pstmt.setString(2, id);
            pstmt.executeUpdate();
            return ResponseEntity.ok(Map.of("message", "Updated successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}