//package com.findear.batch.alarm.controller;
//
//import com.findear.batch.alarm.service.NotificationService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/notification")
//public class NotificationApiController {
//
//    private final NotificationService notificationService;
//
//    @PostMapping("/new")
//    public void saveNotification(@RequestBody String token) {
//        notificationService.saveNotification(token);
//    }
//}