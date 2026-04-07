package com.kernel.kernel_server;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
public class UploadController {

    // The safe folder on the server where files will be stored
    private static final String UPLOAD_DIR = "kernel_cloud_storage/";

    @PostMapping("/api/upload")
    public ResponseEntity<Map<String, String>> handleFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("courseCode") String courseCode,
            @RequestParam("uploaderId") String uploaderId) {

        try {
            // 1. Create the storage directory if it doesn't exist
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 2. Generate a collision-proof filename (e.g., "a1b2c3d4_Lecture1.pdf")
            String safeFileName = UUID.randomUUID().toString().substring(0, 8) + "_" + file.getOriginalFilename().replaceAll("\\s+", "_");
            Path destination = Paths.get(UPLOAD_DIR + safeFileName);

            // 3. Save the actual byte stream to the hard drive
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("☁️ CLOUD UPLOAD SUCCESS: " + uploaderId + " uploaded " + safeFileName + " to " + courseCode);

            // (Optional later step: Insert 'destination.toString()' into your database so students can download it)

            return ResponseEntity.ok(Map.of("message", "File uploaded successfully", "filePath", destination.toString()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file"));
        }
    }
}