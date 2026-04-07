package com.kernel.kernel_server;

import com.kernel.kernel_server.utils.DatabaseHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@RestController
public class DashboardApiController {

    @GetMapping("/api/dashboard/courses")
    public ResponseEntity<List<Map<String, String>>> getDashboardCourses(@RequestParam String role, @RequestParam String name) {
        List<Map<String, String>> courses = new ArrayList<>();
        // Students see all Level-1-Term-2 courses. Teachers see ONLY assigned courses.
        String sql = "STUDENT".equalsIgnoreCase(role)
                ? "SELECT * FROM courses"
                : "SELECT c.* FROM courses c INNER JOIN course_assignments ca ON REPLACE(c.course_code, ' ', '') = REPLACE(ca.course_code, ' ', '') WHERE ca.teacher_name = ?";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (!"STUDENT".equalsIgnoreCase(role)) pstmt.setString(1, name);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, String> course = new HashMap<>();
                course.put("courseCode", rs.getString("course_code"));
                course.put("courseTitle", rs.getString("course_title"));
                courses.add(course);
            }
            return ResponseEntity.ok(courses);
        } catch (Exception e) { return ResponseEntity.internalServerError().build(); }
    }

    @GetMapping("/api/dashboard/deadlines")
    public ResponseEntity<List<Map<String, String>>> getDashboardDeadlines(@RequestParam String role, @RequestParam String name) {
        List<Map<String, String>> deadlines = new ArrayList<>();
        // Filter evaluation deadlines strictly by role and assignment
        String sql = "STUDENT".equalsIgnoreCase(role)
                ? "SELECT * FROM evaluations ORDER BY deadline_date ASC, deadline_time ASC LIMIT 10"
                : "SELECT e.* FROM evaluations e INNER JOIN course_assignments ca ON REPLACE(e.course_code, ' ', '') = REPLACE(ca.course_code, ' ', '') WHERE ca.teacher_name = ? ORDER BY e.deadline_date ASC, e.deadline_time ASC LIMIT 10";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (!"STUDENT".equalsIgnoreCase(role)) pstmt.setString(1, name);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, String> eval = new HashMap<>();
                eval.put("id", rs.getString("id"));
                eval.put("courseCode", rs.getString("course_code"));
                eval.put("title", rs.getString("title"));
                eval.put("type", rs.getString("type"));
                eval.put("deadlineDate", rs.getString("deadline_date"));
                eval.put("deadlineTime", rs.getString("deadline_time"));
                deadlines.add(eval);
            }
            return ResponseEntity.ok(deadlines);
        } catch (Exception e) { return ResponseEntity.internalServerError().build(); }
    }


    @GetMapping("/api/notifications")
    public ResponseEntity<List<Map<String, String>>> getLatestNotifications() {
        List<Map<String, String>> notifications = new ArrayList<>();

        // THE FIX: We are now querying your actual 'assessments' table!
        // We even use SQLite string concatenation to make the message look cool (e.g., "Assignment posted: Week 12 Project")
        String query = "SELECT course_code, assessment_type || ' posted: ' || title AS message FROM assessments ORDER BY id DESC LIMIT 5";

        try (Connection conn = DatabaseHandler.connect(); // Ensure this matches your package import!
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                notifications.add(Map.of(
                        "courseCode", rs.getString("course_code"),
                        "message", rs.getString("message") // Grabbing the concatenated string
                ));
            }
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}