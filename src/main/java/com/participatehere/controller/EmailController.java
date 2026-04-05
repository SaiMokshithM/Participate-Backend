package com.participatehere.controller;

import com.participatehere.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/send")
    public String sendEmail() {
        emailService.sendEmail("participatehere@gmail.com", "Test Email", "<p>Email sent successfully from ParticipateHere!</p>");
        return "email sent Successfully";
    }
}
