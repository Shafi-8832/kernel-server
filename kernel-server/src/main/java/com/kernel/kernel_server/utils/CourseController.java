package com.kernel.kernel_server;

import com.kernel.kernel_server.utils.DatabaseHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class CourseController {

    @GetMapping("/api/courses")
    public ResponseEntity<List<Map<String, Object>>> getAllCourses() {
        List<Map<String, Object>> courses = new ArrayList<>();

        try (Connection conn = DatabaseHandler.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM courses")) {

            while (rs.next()) {
                Map<String, Object> course = new HashMap<>();
                course.put("courseCode", rs.getString("course_code"));
                // 👉 FIXED: "course_title" matches your actual database table
                course.put("courseTitle", rs.getString("course_title"));
                course.put("type", rs.getString("type"));
                course.put("credits", rs.getString("credits"));
                courses.add(course);
            }
            System.out.println("✅ Sent " + courses.size() + " courses to the client.");
            return ResponseEntity.ok(courses);

        } catch (Exception e) {
            System.out.println("❌ Database Crash in CourseController:");
            e.printStackTrace();
            return ResponseEntity.internalServerError().build(); // This sends the 500 error!
        }
    }
}