package com.kernel.kernel_server;

import com.kernel.kernel_server.utils.DatabaseHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.sql.Statement;

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

    @PostMapping("/api/admin/assign-teacher")
    public ResponseEntity<Map<String, String>> assignTeacher(@RequestBody Map<String, String> data) {
        String courseCode = data.get("courseCode");
        String teacherId = data.get("teacherId");
        String teacherName = data.get("teacherName");

        if (courseCode == null || teacherId == null || teacherName == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing fields"));
        }

        try (Connection conn = DatabaseHandler.connect()) {
            // Ensure tables exist
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS teacher_courses (teacher_id TEXT, course_code TEXT)");
                stmt.execute("CREATE TABLE IF NOT EXISTS course_assignments (course_code TEXT, teacher_name TEXT)");
            }

            // Check if already assigned in teacher_courses
            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT 1 FROM teacher_courses WHERE teacher_id = ? AND course_code = ?")) {
                checkStmt.setString(1, teacherId);
                checkStmt.setString(2, courseCode);
                if (checkStmt.executeQuery().next()) {
                    return ResponseEntity.status(409).body(Map.of("error", "Already assigned"));
                }
            }

            // Insert into teacher_courses (by ID)
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO teacher_courses (teacher_id, course_code) VALUES (?, ?)")) {
                pstmt.setString(1, teacherId);
                pstmt.setString(2, courseCode);
                pstmt.executeUpdate();
            }

            // Insert into course_assignments (by name) — this table is used by DashboardApiController
            try (PreparedStatement checkName = conn.prepareStatement(
                    "SELECT 1 FROM course_assignments WHERE course_code = ? AND teacher_name = ?")) {
                checkName.setString(1, courseCode);
                checkName.setString(2, teacherName);
                if (!checkName.executeQuery().next()) {
                    try (PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO course_assignments (course_code, teacher_name) VALUES (?, ?)")) {
                        pstmt.setString(1, courseCode);
                        pstmt.setString(2, teacherName);
                        pstmt.executeUpdate();
                    }
                }
            }

            System.out.println("✅ Assigned " + teacherName + " (" + teacherId + ") → " + courseCode);
            return ResponseEntity.ok(Map.of("message", "Teacher assigned successfully"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Database error"));
        }
    }

    @PostMapping("/api/admin/unassign-teacher")
    public ResponseEntity<Map<String, String>> unassignTeacher(@RequestBody Map<String, String> data) {
        String courseCode = data.get("courseCode");
        String teacherId = data.get("teacherId");
        String teacherName = data.get("teacherName");

        if (courseCode == null || teacherId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing fields"));
        }

        try (Connection conn = DatabaseHandler.connect()) {
            // Remove from teacher_courses (by ID)
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM teacher_courses WHERE teacher_id = ? AND course_code = ?")) {
                pstmt.setString(1, teacherId);
                pstmt.setString(2, courseCode);
                pstmt.executeUpdate();
            }

            // Remove from course_assignments (by name)
            if (teacherName != null) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "DELETE FROM course_assignments WHERE course_code = ? AND teacher_name = ?")) {
                    pstmt.setString(1, courseCode);
                    pstmt.setString(2, teacherName);
                    pstmt.executeUpdate();
                }
            }

            System.out.println("❌ Unassigned " + teacherName + " (" + teacherId + ") from " + courseCode);
            return ResponseEntity.ok(Map.of("message", "Teacher removed from course"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Database error"));
        }
    }

}