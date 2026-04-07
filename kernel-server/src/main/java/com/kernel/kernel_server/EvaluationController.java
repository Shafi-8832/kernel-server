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
import java.util.HashMap;
import java.util.Map;

@RestController
public class EvaluationController {

    private void ensureTablesExist() {
        try (Connection conn = DatabaseHandler.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS evaluations (id INTEGER PRIMARY KEY AUTOINCREMENT, course_code TEXT, title TEXT, type TEXT, target_sections TEXT, total_marks TEXT, start_date TEXT, start_time TEXT, deadline_date TEXT, deadline_time TEXT, description TEXT, creator_id TEXT, file_path TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS announcements (id INTEGER PRIMARY KEY AUTOINCREMENT, course_code TEXT, message TEXT, timestamp TEXT, creator_id TEXT)");
        } catch (Exception ignored) {}
    }

    // CREATE NEW ASSESSMENT
    @PostMapping("/api/evaluations")
    public ResponseEntity<Map<String, String>> createEvaluation(@RequestBody Map<String, String> data) {
        ensureTablesExist();
        String insertSql = "INSERT INTO evaluations (course_code, title, type, target_sections, total_marks, start_date, start_time, deadline_date, deadline_time, description, creator_id, file_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String announceSql = "INSERT INTO announcements (course_code, message, timestamp, creator_id) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(insertSql);
             PreparedStatement astmt = conn.prepareStatement(announceSql)) {

            // 1. Save Assessment
            pstmt.setString(1, data.get("courseCode"));
            pstmt.setString(2, data.get("title"));
            pstmt.setString(3, data.get("type"));
            pstmt.setString(4, data.get("targetSections"));
            pstmt.setString(5, data.get("totalMarks"));
            pstmt.setString(6, data.get("startDate"));
            pstmt.setString(7, data.get("startTime"));
            pstmt.setString(8, data.get("deadlineDate"));
            pstmt.setString(9, data.get("deadlineTime"));
            pstmt.setString(10, data.get("description"));
            pstmt.setString(11, data.get("creatorId"));
            pstmt.setString(12, data.get("filePath"));
            pstmt.executeUpdate();

            // 2. Auto-Broadcast Announcement
            String announcementText = "🔔 New " + data.get("type") + " Published: " + data.get("title") + " (Due: " + data.get("deadlineDate") + " " + data.get("deadlineTime") + ")";
            astmt.setString(1, data.get("courseCode"));
            astmt.setString(2, announcementText);
            astmt.setString(3, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            astmt.setString(4, data.get("creatorId"));
            astmt.executeUpdate();

            System.out.println("✅ Cloud: Created new " + data.get("type") + " -> " + data.get("title"));
            return ResponseEntity.ok(Map.of("message", "Assessment published successfully!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // UPDATE EXISTING ASSESSMENT
    @PutMapping("/api/evaluations/{id}")
    public ResponseEntity<Map<String, String>> updateEvaluation(@PathVariable String id, @RequestBody Map<String, String> data) {
        ensureTablesExist();

        // If file is "None", we don't overwrite the existing file path. If it has a path, we update it.
        boolean updateFile = !data.get("filePath").equals("None");
        String updateSql = updateFile
                ? "UPDATE evaluations SET title=?, type=?, target_sections=?, total_marks=?, start_date=?, start_time=?, deadline_date=?, deadline_time=?, description=?, file_path=? WHERE id=?"
                : "UPDATE evaluations SET title=?, type=?, target_sections=?, total_marks=?, start_date=?, start_time=?, deadline_date=?, deadline_time=?, description=? WHERE id=?";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

            pstmt.setString(1, data.get("title"));
            pstmt.setString(2, data.get("type"));
            pstmt.setString(3, data.get("targetSections"));
            pstmt.setString(4, data.get("totalMarks"));
            pstmt.setString(5, data.get("startDate"));
            pstmt.setString(6, data.get("startTime"));
            pstmt.setString(7, data.get("deadlineDate"));
            pstmt.setString(8, data.get("deadlineTime"));
            pstmt.setString(9, data.get("description"));

            if (updateFile) {
                pstmt.setString(10, data.get("filePath"));
                pstmt.setString(11, id);
            } else {
                pstmt.setString(10, id);
            }

            pstmt.executeUpdate();
            System.out.println("✅ Cloud: Updated Assessment ID: " + id);
            return ResponseEntity.ok(Map.of("message", "Assessment updated successfully!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/evaluations/course/{courseCode}")
    public ResponseEntity<java.util.List<Map<String, String>>> getCourseEvaluations(@PathVariable String courseCode) {
        ensureTablesExist();
        java.util.List<Map<String, String>> evals = new java.util.ArrayList<>();
        String sql = "SELECT id, title, type, start_date FROM evaluations WHERE course_code = ?";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseCode);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, String> eval = new java.util.HashMap<>();
                eval.put("id", rs.getString("id"));
                eval.put("title", rs.getString("title"));
                eval.put("type", rs.getString("type"));
                eval.put("startDate", rs.getString("start_date"));
                evals.add(eval);
            }
            return ResponseEntity.ok(evals);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/evaluations/course/{courseCode}/{type}")
    public ResponseEntity<java.util.List<java.util.Map<String, String>>> getEvaluationsByType(@PathVariable String courseCode, @PathVariable String type) {
        ensureTablesExist();
        java.util.List<java.util.Map<String, String>> evals = new java.util.ArrayList<>();
        // Fetch everything ordered by deadline!
        String sql = "SELECT * FROM evaluations WHERE course_code = ? AND type = ? ORDER BY deadline_date ASC, deadline_time ASC";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseCode);
            pstmt.setString(2, type);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                java.util.Map<String, String> eval = new java.util.HashMap<>();
                eval.put("id", rs.getString("id"));
                eval.put("title", rs.getString("title"));
                eval.put("type", rs.getString("type"));
                eval.put("startDate", rs.getString("start_date"));
                eval.put("startTime", rs.getString("start_time"));
                eval.put("deadlineDate", rs.getString("deadline_date"));
                eval.put("deadlineTime", rs.getString("deadline_time"));
                evals.add(eval);
            }
            return ResponseEntity.ok(evals);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/evaluations/details/{id}")
    public ResponseEntity<Map<String, String>> getEvaluationDetails(@PathVariable String id) {
        ensureTablesExist();
        String sql = "SELECT e.*, u.name AS creator_name FROM evaluations e LEFT JOIN users u ON e.creator_id = u.id WHERE e.id = ?";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Map<String, String> eval = new HashMap<>();
                eval.put("title", rs.getString("title"));
                eval.put("totalMarks", rs.getString("total_marks"));
                eval.put("description", rs.getString("description"));
                eval.put("type", rs.getString("type"));
                eval.put("creatorName", rs.getString("creator_name") != null ? rs.getString("creator_name") : "Unknown Instructor");
                eval.put("creatorId", rs.getString("creator_id"));
                eval.put("filePath", rs.getString("file_path"));
                eval.put("startDate", rs.getString("start_date"));
                eval.put("startTime", rs.getString("start_time"));
                eval.put("deadlineDate", rs.getString("deadline_date"));
                eval.put("deadlineTime", rs.getString("deadline_time"));
                return ResponseEntity.ok(eval);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}