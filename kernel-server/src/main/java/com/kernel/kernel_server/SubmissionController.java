package com.kernel.kernel_server;

import com.kernel.kernel_server.utils.DatabaseHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class SubmissionController {

    private void ensureTableExists() {
        try (Connection conn = DatabaseHandler.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS submissions (id INTEGER PRIMARY KEY AUTOINCREMENT, evaluation_id INTEGER, student_id TEXT, file_path TEXT, submission_time TEXT, grade TEXT)");
        } catch (Exception ignored) {}
    }

    @PostMapping("/api/submissions")
    public ResponseEntity<Map<String, String>> recordSubmission(@RequestBody Map<String, String> data) {
        ensureTableExists();
        String checkSql = "SELECT id FROM submissions WHERE evaluation_id=? AND student_id=?";
        String updateSql = "UPDATE submissions SET file_path = ?, submission_time = ? WHERE evaluation_id = ? AND student_id = ?";
        String insertSql = "INSERT INTO submissions (file_path, submission_time, evaluation_id, student_id, grade) VALUES (?, ?, ?, ?, 'Not Graded')";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setString(1, data.get("assessmentId"));
            checkStmt.setString(2, data.get("studentId"));
            boolean exists = checkStmt.executeQuery().next();

            String sql = exists ? updateSql : insertSql;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, data.get("filePath"));
                pstmt.setString(2, LocalDateTime.now().toString());
                pstmt.setString(3, data.get("assessmentId"));
                pstmt.setString(4, data.get("studentId"));
                pstmt.executeUpdate();
            }
            return ResponseEntity.ok(Map.of("message", "Submission saved to cloud"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/submissions/{evalId}/{studentId}")
    public ResponseEntity<Map<String, String>> getSubmission(@PathVariable String evalId, @PathVariable String studentId) {
        ensureTableExists();
        String sql = "SELECT file_path, grade FROM submissions WHERE evaluation_id = ? AND student_id = ?";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, evalId);
            pstmt.setString(2, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return ResponseEntity.ok(Map.of(
                        "filePath", rs.getString("file_path"),
                        "grade", rs.getString("grade") != null ? rs.getString("grade") : "Not Graded"
                ));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/api/submissions/{evalId}/{studentId}")
    public ResponseEntity<Void> deleteSubmission(@PathVariable String evalId, @PathVariable String studentId) {
        ensureTableExists();
        String sql = "DELETE FROM submissions WHERE evaluation_id = ? AND student_id = ?";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, evalId);
            pstmt.setString(2, studentId);
            pstmt.executeUpdate();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/evaluations/{evalId}/submissions")
    public ResponseEntity<java.util.List<Map<String, String>>> getAllSubmissionsForEval(@PathVariable String evalId) {
        ensureTableExists();
        java.util.List<Map<String, String>> submissions = new java.util.ArrayList<>();
        String sql = "SELECT s.file_path, s.submission_time, s.grade, u.name, u.id as roll " +
                "FROM submissions s JOIN users u ON s.student_id = u.id " +
                "WHERE s.evaluation_id = ?";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, evalId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, String> sub = new java.util.HashMap<>();
                sub.put("filePath", rs.getString("file_path"));
                sub.put("submissionTime", rs.getString("submission_time"));
                sub.put("grade", rs.getString("grade"));
                sub.put("name", rs.getString("name"));
                sub.put("roll", rs.getString("roll"));
                submissions.add(sub);
            }
            return ResponseEntity.ok(submissions);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/api/submissions/grades")
    public ResponseEntity<Map<String, String>> updateGrades(@RequestBody java.util.List<Map<String, String>> grades) {
        ensureTableExists();
        String sql = "UPDATE submissions SET grade = ? WHERE student_id = ? AND evaluation_id = ?";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int count = 0;
            for (Map<String, String> g : grades) {
                pstmt.setString(1, g.get("grade"));
                pstmt.setString(2, g.get("studentId"));
                pstmt.setString(3, g.get("evaluationId"));
                pstmt.addBatch();
                count++;
            }
            pstmt.executeBatch();
            System.out.println("✅ Cloud: Updated " + count + " grades.");
            return ResponseEntity.ok(Map.of("message", "Grades updated successfully", "count", String.valueOf(count)));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}