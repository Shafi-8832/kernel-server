package com.kernel.kernel_server;

import com.kernel.kernel_server.utils.DatabaseHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class DashboardController {

    @GetMapping("/api/dashboard/urgent")
    public ResponseEntity<List<Map<String, String>>> getUrgentDeadlines() {
        List<Map<String, String>> urgentTasks = new ArrayList<>();
        String today = LocalDate.now().toString();

        try (Connection conn = DatabaseHandler.connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS evaluations (id INTEGER PRIMARY KEY AUTOINCREMENT, course_code TEXT, title TEXT, type TEXT, target_sections TEXT, total_marks TEXT, start_date TEXT, start_time TEXT, deadline_date TEXT, deadline_time TEXT, description TEXT, creator_id TEXT, file_path TEXT)");

            // Now fetching ID as well
            // Added 'type' to the SELECT query
            String sql = "SELECT id, title, type, deadline_date, deadline_time, course_code FROM evaluations WHERE deadline_date >= '" + today + "' ORDER BY deadline_date ASC LIMIT 5";
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Map<String, String> task = new HashMap<>();
                String code = rs.getString("course_code") != null ? rs.getString("course_code") : "Course";
                String title = rs.getString("title") != null ? rs.getString("title") : "Assessment";
                String date = rs.getString("deadline_date") != null ? rs.getString("deadline_date") : "TBD";
                String time = rs.getString("deadline_time") != null ? rs.getString("deadline_time") : "23:59";

                task.put("id", rs.getString("id"));
                task.put("title", code + " - " + title);
                task.put("type", rs.getString("type") != null ? rs.getString("type") : "UNKNOWN"); // 👉 NEW!
                task.put("dueDate", date);
                task.put("dueTime", time);
                urgentTasks.add(task);
            }
            return ResponseEntity.ok(urgentTasks);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }
}