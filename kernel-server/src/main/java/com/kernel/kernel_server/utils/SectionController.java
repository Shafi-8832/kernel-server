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
public class SectionController {

    @GetMapping("/api/sections/{courseCode}")
    public ResponseEntity<List<Map<String, Object>>> getCourseSections(@PathVariable String courseCode) {
        List<Map<String, Object>> sections = new ArrayList<>();
        String decodedCode = java.net.URLDecoder.decode(courseCode, java.nio.charset.StandardCharsets.UTF_8);

        // First, get all the sections (Weeks)
        String sqlSections = "SELECT * FROM course_sections WHERE course_code = ? ORDER BY week_number ASC";
        // Second, get all the items inside that week
        String sqlModules = "SELECT * FROM course_modules WHERE section_id = ?";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmtSections = conn.prepareStatement(sqlSections);
             PreparedStatement pstmtModules = conn.prepareStatement(sqlModules)) {

            pstmtSections.setString(1, decodedCode);
            ResultSet rsSections = pstmtSections.executeQuery();

            while (rsSections.next()) {
                Map<String, Object> section = new HashMap<>();
                int sectionId = rsSections.getInt("section_id");

                section.put("id", sectionId);
                section.put("title", rsSections.getString("title"));
                section.put("weekNumber", rsSections.getInt("week_number"));
                section.put("flairType", rsSections.getString("flair_type"));

                // Now fetch the modules for THIS specific section
                List<Map<String, Object>> modules = new ArrayList<>();
                pstmtModules.setInt(1, sectionId);
                ResultSet rsModules = pstmtModules.executeQuery();

                while (rsModules.next()) {
                    Map<String, Object> module = new HashMap<>();
                    module.put("id", rsModules.getInt("module_id"));
                    module.put("type", rsModules.getString("module_type"));
                    module.put("title", rsModules.getString("title"));
                    module.put("description", rsModules.getString("description"));
                    module.put("fileLink", rsModules.getString("file_link"));
                    module.put("dueDate", rsModules.getString("due_date"));
                    modules.add(module);
                }

                section.put("modules", modules); // Attach the modules to the section!
                sections.add(section);
            }

            System.out.println("✅ Sent " + sections.size() + " sections for " + decodedCode);
            return ResponseEntity.ok(sections);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}