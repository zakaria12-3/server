package com.example.controller;

import com.example.model.Application;
import com.example.model.Role;
import com.example.model.User;
import com.example.repository.ApplicationRepository;
import com.example.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/files")
public class FileController {
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public FileController(ApplicationRepository applicationRepository, UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/cv/{filename:.+}")
    public ResponseEntity<Resource> getCv(@PathVariable("filename") String filename, Authentication authentication) throws Exception {
        if (!canViewCv(filename, authentication)) {
            return ResponseEntity.status(403).build();
        }

        Path uploadDirPath = Paths.get("uploads/cv").toAbsolutePath().normalize();
        Path path = uploadDirPath.resolve(filename).normalize();

        if (!path.startsWith(uploadDirPath)) {
            return ResponseEntity.status(403).build();
        }

        Resource resource = new UrlResource(Objects.requireNonNull(path.toUri()));

        if (!resource.exists() || !resource.isReadable()) {
            String errorHtml = "<html><body style='font-family: sans-serif; display: flex; align-items: center; justify-content: center; height: 100%; margin: 0; background-color: #f87171; color: white; text-align: center;'><div><h2>CV File Not Found</h2><p>The PDF file for this candidate is missing from the server.</p></div></body></html>";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "text/html")
                    .body(new org.springframework.core.io.ByteArrayResource(errorHtml.getBytes()));
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

    private boolean canViewCv(String filename, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return false;
        }
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ROLE_ADMIN) {
            return true;
        }

        Application app = applicationRepository.findByCvPath(filename).orElse(null);
        if (app == null) {
            return false;
        }
        if (user.getRole() == Role.ROLE_CANDIDATE) {
            return app.getCandidate() != null && Objects.equals(app.getCandidate().getId(), user.getId());
        }
        if (user.getRole() == Role.ROLE_RECRUITER) {
            return app.getJob() != null
                    && app.getJob().getRecruiter() != null
                    && Objects.equals(app.getJob().getRecruiter().getId(), user.getId());
        }
        return false;
    }
}
