package com.example.controller;

import com.example.dto.CreateReportDto;
import com.example.dto.ReportDto;
import com.example.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<ReportDto> createReport(@RequestBody CreateReportDto request, Authentication authentication) {
        return ResponseEntity.ok(reportService.createReport(authentication.getName(), request));
    }
}
