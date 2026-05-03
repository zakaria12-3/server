package com.example.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/files")
public class FileController {

    @GetMapping("/cv/{filename}")
    public ResponseEntity<Resource> getCv(@PathVariable("filename") String filename) throws Exception {

        Path uploadDirPath = Paths.get("uploads/cv").toAbsolutePath().normalize();
        Path path = Paths.get("uploads/cv").resolve(filename).toAbsolutePath().normalize();

        if (!path.startsWith(uploadDirPath)) {
            return ResponseEntity.status(403).build();
        }

        Resource resource = new UrlResource(Objects.requireNonNull(path.toUri()));

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String contentType;
        try {
            contentType = java.nio.file.Files.probeContentType(path);
        } catch (Exception e) {
            contentType = null;
        }

        if (contentType == null) {
            if (filename.toLowerCase().endsWith(".pdf")) {
                contentType = "application/pdf";
            } else {
                contentType = "application/octet-stream";
            }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);

    }
}
