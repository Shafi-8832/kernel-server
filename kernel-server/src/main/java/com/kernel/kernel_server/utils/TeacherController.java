package com.kernel.kernel_server;

import com.kernel.kernel_server.utils.DatabaseHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class TeacherController {

    @GetMapping("/api/courses/{courseCode}/teachers")
    public ResponseEntity<List<Map<String, String>>> getTeachersByCourse(@PathVariable String courseCode) {
        List<Map<String, String>> teachers = new ArrayList<>();
        String decodedCode = java.net.URLDecoder.decode(courseCode, java.nio.charset.StandardCharsets.UTF_8);

        // SQL: Join the users table with the teacher_courses table
        String query = "SELECT u.id, u.name, u.email FROM users u " +
                "JOIN teacher_courses tc ON u.id = tc.teacher_id " +
                "WHERE tc.course_code = ?";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, decodedCode);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, String> t = new HashMap<>();
                t.put("id", rs.getString("id"));
                t.put("name", rs.getString("name"));
                t.put("email", rs.getString("email"));
                t.put("role", "TEACHER");
                teachers.add(t);
            }
            return ResponseEntity.ok(teachers);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}