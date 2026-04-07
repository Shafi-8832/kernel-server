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
public class CourseResourceController {

    private void ensureTableExists() {
        try (Connection conn = DatabaseHandler.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS course_resources (id INTEGER PRIMARY KEY AUTOINCREMENT, course_code TEXT, uploader_id TEXT, uploader_name TEXT, title TEXT, resource_url TEXT, resource_type TEXT, timestamp TEXT)");
        } catch (Exception ignored) {}
    }

    @GetMapping("/api/resources/{courseCode}")
    public ResponseEntity<List<Map<String, String>>> getResources(@PathVariable String courseCode) {
        ensureTableExists();
        List<Map<String, String>> resources = new ArrayList<>();
        String sql = "SELECT * FROM course_resources WHERE course_code = ? ORDER BY id DESC";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseCode);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, String> res = new HashMap<>();
                res.put("id", rs.getString("id"));
                res.put("uploaderId", rs.getString("uploader_id"));
                res.put("uploaderName", rs.getString("uploader_name"));
                res.put("title", rs.getString("title"));
                res.put("resourceUrl", rs.getString("resource_url"));
                res.put("resourceType", rs.getString("resource_type")); // 'FILE' or 'LINK'
                res.put("timestamp", rs.getString("timestamp"));
                resources.add(res);
            }
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/api/resources")
    public ResponseEntity<Map<String, String>> addResource(@RequestBody Map<String, String> data) {
        ensureTableExists();
        String sql = "INSERT INTO course_resources (course_code, uploader_id, uploader_name, title, resource_url, resource_type, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, data.get("courseCode"));
            pstmt.setString(2, data.get("uploaderId"));
            pstmt.setString(3, data.get("uploaderName"));
            pstmt.setString(4, data.get("title"));
            pstmt.setString(5, data.get("resourceUrl"));
            pstmt.setString(6, data.get("resourceType"));
            pstmt.setString(7, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            pstmt.executeUpdate();
            return ResponseEntity.ok(Map.of("message", "Uploaded"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/api/resources/{id}")
    public ResponseEntity<Map<String, String>> deleteResource(@PathVariable String id) {
        String sql = "DELETE FROM course_resources WHERE id = ?";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}