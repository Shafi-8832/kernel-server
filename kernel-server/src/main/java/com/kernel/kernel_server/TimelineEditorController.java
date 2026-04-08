package com.kernel.kernel_server;

import com.kernel.kernel_server.utils.DatabaseHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class TimelineEditorController {

    private void ensureTablesExist() {
        try (Connection conn = DatabaseHandler.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS course_sections (section_id INTEGER PRIMARY KEY AUTOINCREMENT, course_code TEXT, title TEXT, week_number INTEGER, flair_type TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS course_modules (module_id INTEGER PRIMARY KEY AUTOINCREMENT, section_id INTEGER, module_type TEXT, title TEXT, description TEXT, file_link TEXT, is_active INTEGER DEFAULT 1, due_date TEXT)");
            // NEW: Table to save the Teacher's Lab Day preference!
            stmt.execute("CREATE TABLE IF NOT EXISTS course_settings (course_code TEXT PRIMARY KEY, lab_day TEXT)");
        } catch (Exception ignored) {}
    }

    @GetMapping("/api/timeline/{courseCode}")
    public ResponseEntity<Map<String, Object>> getTimeline(@PathVariable String courseCode) {
        ensureTablesExist();
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> sections = new ArrayList<>();
        String labDay = "Thursday"; // Default

        try (Connection conn = DatabaseHandler.connect()) {
            // 1. Fetch Course Settings (Lab Day)
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT lab_day FROM course_settings WHERE course_code = ?")) {
                pstmt.setString(1, courseCode);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) labDay = rs.getString("lab_day");
            }

            // 2. 🚀 THE 14-WEEK ENFORCER (Fills in ANY missing weeks)
            java.util.Set<Integer> existingWeeks = new java.util.HashSet<>();
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT week_number FROM course_sections WHERE course_code = ?")) {
                pstmt.setString(1, courseCode);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) existingWeeks.add(rs.getInt("week_number"));
            }

            String insertSql = "INSERT INTO course_sections (course_code, title, week_number, flair_type) VALUES (?, ?, ?, 'Standard')";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                boolean addedNew = false;
                for (int i = 1; i <= 14; i++) {
                    if (!existingWeeks.contains(i)) {
                        insertStmt.setString(1, courseCode);
                        insertStmt.setString(2, "Week " + i);
                        insertStmt.setInt(3, i);
                        insertStmt.addBatch();
                        addedNew = true;
                    }
                }
                if (addedNew) {
                    insertStmt.executeBatch();
                    System.out.println("✅ Cloud: Enforced 14 Weeks for " + courseCode);
                }
            }

            // 3. Fetch the Final 14 Weeks
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM course_sections WHERE course_code = ? ORDER BY week_number ASC")) {
                pstmt.setString(1, courseCode);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    Map<String, Object> sec = new HashMap<>();
                    sec.put("id", rs.getInt("section_id"));
                    sec.put("title", rs.getString("title"));
                    sec.put("weekNumber", rs.getInt("week_number"));
                    sec.put("flairType", rs.getString("flair_type"));
                    sections.add(sec);
                }
            }

            response.put("labDay", labDay);
            response.put("sections", sections);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // 🚀 NEW: API to save the Lab Day
    @PostMapping("/api/timeline/settings")
    public ResponseEntity<Map<String, String>> updateSettings(@RequestBody Map<String, String> data) {
        ensureTablesExist();
        String sql = "INSERT INTO course_settings (course_code, lab_day) VALUES (?, ?) ON CONFLICT(course_code) DO UPDATE SET lab_day=excluded.lab_day";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, data.get("courseCode"));
            pstmt.setString(2, data.get("labDay"));
            pstmt.executeUpdate();
            System.out.println("✅ Cloud: Lab Day updated to " + data.get("labDay"));
            return ResponseEntity.ok(Map.of("message", "Saved"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/api/sections/rename")
    public ResponseEntity<Map<String, String>> renameSection(@RequestBody Map<String, String> data) {
        ensureTablesExist();
        String sql = "UPDATE course_sections SET title = ? WHERE section_id = ?";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, data.get("title"));
            pstmt.setString(2, data.get("sectionId"));
            pstmt.executeUpdate();
            System.out.println("✅ Cloud: Section renamed to '" + data.get("title") + "'");
            return ResponseEntity.ok(Map.of("message", "Renamed"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // KEEP existing createSection and createModule methods below...
    @PostMapping("/api/sections")
    public ResponseEntity<Map<String, String>> createSection(@RequestBody Map<String, Object> data) {
        // [Keep your existing createSection code here]
        return ResponseEntity.ok(Map.of("message", "Week added successfully"));
    }

    @PostMapping("/api/modules")
    public ResponseEntity<Map<String, String>> createModule(@RequestBody Map<String, Object> data) {
        // [Keep your existing createModule code here]
        return ResponseEntity.ok(Map.of("message", "Module added successfully"));
    }


}