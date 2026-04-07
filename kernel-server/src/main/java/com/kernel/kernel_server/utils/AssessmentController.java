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
public class AssessmentController {

    // Notice the {courseCode} in the URL! This makes it dynamic.
    @GetMapping("/api/assessments/{courseCode}")
    public ResponseEntity<List<Map<String, Object>>> getCourseAssessments(@PathVariable String courseCode) {
        List<Map<String, Object>> assessments = new ArrayList<>();

        String sql = "SELECT * FROM assessments WHERE course_code = ? ORDER BY week_number ASC";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Decode the URL format (e.g., "CSE%20105" back to "CSE 105")
            String decodedCode = java.net.URLDecoder.decode(courseCode, java.nio.charset.StandardCharsets.UTF_8);
            pstmt.setString(1, decodedCode);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", rs.getInt("id"));
                data.put("courseCode", rs.getString("course_code"));
                data.put("weekNumber", rs.getInt("week_number"));
                data.put("title", rs.getString("title"));
                data.put("dateTime", rs.getString("date_time"));
                data.put("room", rs.getString("room"));
                data.put("assessmentType", rs.getString("assessment_type"));

                // Pack the extra details so the frontend has everything
                data.put("syllabus", rs.getString("syllabus"));
                data.put("totalMarks", rs.getInt("total_marks"));
                data.put("submissionLink", rs.getString("submission_link"));
                data.put("duration", rs.getString("duration"));

                assessments.add(data);
            }
            System.out.println("✅ Sent " + assessments.size() + " assessments for " + decodedCode);
            return ResponseEntity.ok(assessments);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}