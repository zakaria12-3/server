package com.example.config;

import com.example.service.ActionLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ActionLogInterceptor implements HandlerInterceptor {
    private final ActionLogService actionLogService;

    public ActionLogInterceptor(ActionLogService actionLogService) {
        this.actionLogService = actionLogService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            actionLogService.logRequest(request, response, SecurityContextHolder.getContext().getAuthentication());
        } catch (Exception ignored) {
        }
    }
}
