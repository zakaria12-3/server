package com.example.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/meet")
public class MeetingController {
    @PostMapping("/create")
    public String createMeeting(@RequestParam Long jobId) {



        String roomName = "job-" + jobId + "-" + System.currentTimeMillis();
        return "https://meet.jit.si/" + roomName;
    }
}
