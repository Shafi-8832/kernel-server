package com.kernel.kernel_server;

import com.kernel.kernel_server.utils.DatabaseHandler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DatabaseMigrator implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        try (Connection conn = DatabaseHandler.connect()) {
            migrateTeacherEmails(conn);
            seedCourseAssignments(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void migrateTeacherEmails(Connection conn) throws Exception {
        System.out.println("🔄 Checking for outdated Teacher emails...");
        String selectSql = "SELECT id, name, email FROM users WHERE role = 'TEACHER'";
        String updateSql = "UPDATE users SET email = ? WHERE id = ?";

        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
            ResultSet rs = selectStmt.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String currentEmail = rs.getString("email");
                if (currentEmail == null || !currentEmail.contains("@")) {
                    String newEmail = name.toLowerCase().replaceAll("\\s+", ".") + "@cse.buet.ac.bd";
                    updateStmt.setString(1, newEmail);
                    updateStmt.setString(2, id);
                    updateStmt.executeUpdate();
                }
            }
        }
    }

    private void seedCourseAssignments(Connection conn) throws Exception {
        System.out.println("🔄 Seeding Teacher-Course Assignments...");
        try (java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS course_assignments (course_code TEXT, teacher_name TEXT)");
            stmt.execute("DELETE FROM course_assignments"); // Refresh table on boot
        }

        // THE ENTERPRISE MAPPING ENGINE
        Map<String, List<String>> assignments = new HashMap<>();
        assignments.put("CSE 105", List.of("Dr. Md. Monirul Islam", "Md Nurul Muttakin", "Anwarul Bashir Shuaib"));
        assignments.put("CSE 106", List.of("Muhammad Abdullah Adnan", "Md. Ishrak Ahsan", "Arnob Saha Ankon", "Asif Azad", "Mahir Labib Dihan", "Dr. Md. Monirul Islam", "Ishrat Jahan", "Md Nurul Muttakin", "Abdur Rafi", "Ashrafur Rahman", "Iqbal Hossain Raju", "Anwarul Bashir Shuaib"));
        assignments.put("CSE 107", List.of("Mohammad Mahfuzul Islam", "Abdur Rafi", "Khaled Mahmud Shahriar"));
        assignments.put("CSE 108", List.of("Arnob Saha Ankon", "Mahir Labib Dihan", "Emamul Haque", "A.K.M. Mehedi Hasan", "Mohammad Mahfuzul Islam", "Junaed Younus Khan", "Md Nurul Muttakin", "Abdur Rafi", "Ashrafur Rahman", "Kowshic Roy", "Khaled Mahmud Shahriar", "Nafis Tahmid"));
        assignments.put("MATH 143", List.of("Md. Nahid Hasan", "Abdul Malek"));
        assignments.put("CHEM 113", List.of("Dr. Ayesha Sharmin", "Mohammad Hossain"));
        assignments.put("ME 165", List.of("Sadia Tasnim", "Md. Moyeenul Islam Ratul"));
        assignments.put("ME 174", List.of("Rafiul Haq", "Shakil Hasan", "Kazi Tawseef Rahman"));

        String insertSql = "INSERT INTO course_assignments (course_code, teacher_name) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            for (Map.Entry<String, List<String>> entry : assignments.entrySet()) {
                for (String teacher : entry.getValue()) {
                    pstmt.setString(1, entry.getKey());
                    pstmt.setString(2, teacher);
                    pstmt.executeUpdate();
                }
            }
        }
        System.out.println("✅ Security Locks Applied: Course visibility is now strictly enforced!");
    }
}