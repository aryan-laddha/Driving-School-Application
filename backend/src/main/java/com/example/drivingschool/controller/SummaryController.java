package com.example.drivingschool.controller;

import com.example.drivingschool.dto.AdminSummaryDto;
import com.example.drivingschool.service.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/summary")
public class SummaryController {

    @Autowired
    private SummaryService summaryService;

    @GetMapping("/counts")
    public AdminSummaryDto getCounts() {
        return summaryService.getAdminSummary();
    }
}