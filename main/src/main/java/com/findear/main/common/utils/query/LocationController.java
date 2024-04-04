package com.findear.main.common.utils.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/location")
@RestController
public class LocationController {
    private static final String KEY = "03A85DB3-4BC6-3E04-A597-CFDD044A271A";

    @GetMapping(value = "/search", produces = "application/json; charset=UTF8")
    public ResponseEntity<?> search(LocationSearchReqDto locationSearchReqDto) throws IOException {
        log.info(locationSearchReqDto.toString());

        StringBuilder urlBuilder = new StringBuilder("https://api.vworld.kr/req/search?");
        urlBuilder.append("&" + URLEncoder.encode("key","UTF-8") + "=" + URLEncoder.encode(KEY, "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("service","UTF-8") + "=" + URLEncoder.encode("search", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("request","UTF-8") + "=" + URLEncoder.encode("search", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("version","UTF-8") + "=" + URLEncoder.encode("2.0", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("crs","UTF-8") + "=" + URLEncoder.encode("epsg:4326", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("size","UTF-8") + "=" + URLEncoder.encode(String.valueOf(locationSearchReqDto.getSize()), "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("page","UTF-8") + "=" + URLEncoder.encode(String.valueOf(locationSearchReqDto.getPage()), "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("query","UTF-8") + "=" + URLEncoder.encode(locationSearchReqDto.getQuery(), "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("type","UTF-8") + "=" + URLEncoder.encode("PLACE", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("format","UTF-8") + "=" + URLEncoder.encode("json", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("errorformat","UTF-8") + "=" + URLEncoder.encode("json", "UTF-8"));

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json; charset=utf-8");
        BufferedReader rd;
        if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();

        log.info(sb.toString());

        return ResponseEntity.ok(sb);
    }

    @GetMapping(value = "/address", produces = "application/json; charset=UTF8")
    public ResponseEntity<?> address(LocationAddressReqDto locationAddressReqDto) throws IOException {
        log.info(locationAddressReqDto.toString());

        StringBuilder urlBuilder = new StringBuilder("https://api.vworld.kr/req/address?");
        urlBuilder.append("&" + URLEncoder.encode("key","UTF-8") + "=" + URLEncoder.encode(KEY, "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("service","UTF-8") + "=" + URLEncoder.encode("address", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("request","UTF-8") + "=" + URLEncoder.encode("getcoord", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("version","UTF-8") + "=" + URLEncoder.encode("2.0", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("crs","UTF-8") + "=" + URLEncoder.encode("epsg:4326", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("address","UTF-8") + "=" + URLEncoder.encode(locationAddressReqDto.getAddress(), "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("refine","UTF-8") + "=" + URLEncoder.encode("true", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("simple","UTF-8") + "=" + URLEncoder.encode("false", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("type","UTF-8") + "=" + URLEncoder.encode("road", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("format","UTF-8") + "=" + URLEncoder.encode("json", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("errorformat","UTF-8") + "=" + URLEncoder.encode("json", "UTF-8"));

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json; charset=utf-8");

        BufferedReader rd;
        if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();

        log.info(sb.toString());

        return ResponseEntity.ok(sb);
    }
}
