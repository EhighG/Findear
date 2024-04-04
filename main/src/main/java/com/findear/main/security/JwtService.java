package com.findear.main.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.function.Function;

@Component
public class JwtService {

    private final String SECRET;
    private final int TOKEN_DURATION_SEC;

    public static final String TOKEN_TYPE = "bearer";

    public JwtService(@Value("${jwt-secret}") String secret,
                      @Value("${jwt-duration}") int tokenDuration) {
        SECRET = secret;
        TOKEN_DURATION_SEC = tokenDuration;
    }


    public Date getExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Long getMemberId(String token) {
        String memberIdStr = extractClaim(token, Claims::getSubject);
        if (memberIdStr == null) {
            throw new RuntimeException("잘못된 accessToken");
        }
        return Long.parseLong(memberIdStr);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignKey()) // 디코딩할 키
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isExpired(String token) {
        try {
            return getExpiration(token).before(new Date());
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    /**
     * uesrId에 대한 검증은 이미 마친 상태에서 호출된다.
     */
    public String createAccessToken(Long memberId) {
        return Jwts.builder()
                .setClaims(new HashMap<>())
                .setSubject(memberId.toString())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_DURATION_SEC)) // 1시간
                .signWith(getSignKey(), SignatureAlgorithm.HS256).compact();
    }

    public String createRefreshToken(Long memberId) {
        return Jwts.builder()
                .setClaims(new HashMap<>())
                .setSubject(memberId.toString())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + (TOKEN_DURATION_SEC * 24L))) // 1시간
                .signWith(getSignKey(), SignatureAlgorithm.HS256).compact();
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes); // HMAC : Hash-based Message Authentication Code / 주어진 값과 해시함수로 키 생성
    }
}