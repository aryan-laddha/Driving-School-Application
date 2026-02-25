package com.example.drivingschool.controller;

import com.example.drivingschool.service.WhatsAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    @Autowired
    private WhatsAppService whatsAppService;

    @GetMapping("/test")
    public String sendTestMessage(@RequestParam String to) {
        return whatsAppService.sendTemplateMessage(to);
    }
}
