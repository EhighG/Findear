package com.findear.main.member.command.service;

import com.findear.main.member.command.dto.NaverAccessTokenResponse;
import com.findear.main.member.command.dto.NaverMemberInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverOAuthProvider {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String TOKEN_TYPE = "Bearer ";

    @Value("${auth.naver.token-request-uri}")
    private String tokenRequestUri;

    @Value("${auth.naver.member-info-request-uri}")
    private String memberInfoRequestUri;

    @Value("${auth.naver.redirect-uri}")
    private String redirectUri;

    @Value("${auth.naver.client-id}")
    private String clientId;

    @Value("${auth.naver.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;

    public NaverAccessTokenResponse getAccessToken(String authCode, String state) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", authCode);
        body.add("client_secret", clientSecret);
        body.add("state", state);

        return sendAccessTokenRequest(body, MediaType.APPLICATION_FORM_URLENCODED);
    }

    public NaverAccessTokenResponse refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        try {
            NaverAccessTokenResponse accessTokenResponse = sendAccessTokenRequest(body, MediaType.APPLICATION_FORM_URLENCODED);
            if (accessTokenResponse.getErrorCode() != null) {
                log.info("액세스토큰 요청에서 오류 발생!! errMessage = " + accessTokenResponse.getErrorDescription());
                log.info("error code = " + accessTokenResponse.getErrorCode());
                throw new Exception();
            }
            return accessTokenResponse;
        } catch (Exception e) {
            throw new AuthenticationServiceException("refreshToken이 안 맞는 경우. Naver 재로그인 필요");
        }
    }

    private NaverAccessTokenResponse sendAccessTokenRequest(MultiValueMap<String, String> body,
                                                            MediaType contentType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            return restTemplate.postForEntity(tokenRequestUri, request, NaverAccessTokenResponse.class).getBody();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("accessToken 요청 중 에러");
        }
    }

    public NaverMemberInfoDto getMemberInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(AUTHORIZATION_HEADER, TOKEN_TYPE + accessToken);

            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    memberInfoRequestUri,
                    HttpMethod.POST,
                    new HttpEntity<>(headers),
                    Map.class
            );
            Map<String, String> response = (Map<String, String>) responseEntity.getBody().get("response");
            return new NaverMemberInfoDto(response.get("uid"),
                    response.get("mobile"),
                    response.get("age"),
                    response.get("gender")
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
