package com.example.service;

import com.example.model.ActionLog;
import com.example.model.User;
import com.example.repository.ActionLogRepository;
import com.example.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActionLogService {
    private final ActionLogRepository actionLogRepository;
    private final UserRepository userRepository;

    public ActionLogService(ActionLogRepository actionLogRepository, UserRepository userRepository) {
        this.actionLogRepository = actionLogRepository;
        this.userRepository = userRepository;
    }

    public List<ActionLog> getRecentLogs(String role) {
        if (role == null || role.isBlank() || "ALL".equalsIgnoreCase(role)) {
            return actionLogRepository.findAllRecent();
        }
        String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
        return actionLogRepository.findTop200ByRoleOrderByCreatedAtDesc(normalizedRole);
    }

    public void logRequest(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }

        String path = request.getRequestURI();
        if (path.startsWith("/admin/action-logs")) {
            return;
        }

        ActionLog log = new ActionLog();
        log.setActorEmail(authentication.getName());
        log.setMethod(request.getMethod());
        log.setPath(path);
        log.setStatus(response.getStatus());
        log.setRole(authentication.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("UNKNOWN"));
        log.setAction(buildAction(request.getMethod(), path));

        userRepository.findByEmail(authentication.getName()).ifPresent(user -> {
            log.setActorName(user.getRealUsername());
            if (user.getRole() != null) {
                log.setRole(user.getRole().name());
            }
        });

        actionLogRepository.save(log);
    }

    private String buildAction(String method, String path) {
        String target = path
                .replaceFirst("^/(admin|candidate|recruiter|users|posts|messages|search|chat)/?", "")
                .replace("/", " ");
        if (target.isBlank()) {
            target = "dashboard";
        }

        return switch (method) {
            case "POST" -> "Created or submitted " + target;
            case "PUT", "PATCH" -> "Updated " + target;
            case "DELETE" -> "Deleted " + target;
            default -> "Viewed " + target;
        };
    }
}
