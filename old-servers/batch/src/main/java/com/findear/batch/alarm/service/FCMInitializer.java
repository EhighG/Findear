//package com.findear.batch.alarm.service;
//
//import com.google.auth.oauth2.GoogleCredentials;
//import com.google.firebase.FirebaseApp;
//import com.google.firebase.FirebaseOptions;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.PostConstruct;
//import java.io.IOException;
//
//
//@Slf4j
//@Service
//public class FCMInitializer {
//
//    private static final String FIREBASE_CONFIG_PATH = "findear-bfd63-firebase-adminsdk-9a4ao-95dc95086e.json";
//
//    @PostConstruct
//    public void initialize() {
//        try {
//            GoogleCredentials googleCredentials = GoogleCredentials
//                    .fromStream(new ClassPathResource(FIREBASE_CONFIG_PATH).getInputStream());
//            FirebaseOptions options = new FirebaseOptions.Builder()
//                    .setCredentials(googleCredentials)
//                    .build();
//            FirebaseApp.initializeApp(options);
//        } catch (IOException e) {
//            log.info(">>>>>>>>FCM error");
//            log.error(">>>>>>FCM error message : " + e.getMessage());
//        }
//    }
//}