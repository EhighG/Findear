package com.findear.main.security;

import org.apache.catalina.filters.CorsFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtFilter jwtFilter,
                                           JwtExceptionFilter jwtExceptionFilter,
                                           JwtAuthenticationEntryPoint entryPoint) throws Exception {

        http
//                .cors(corsCustomizer -> corsCustomizer.configurationSource(corsConfigurationSource()))
                .cors(cors -> cors.disable())
                .csrf(AbstractHttpConfigurer::disable);
        http
                .addFilterAt(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/members/login", "/members/emails/**", "/members/find-password", "/actuator/**", "/members/duplicate", "/error", "/assets/**",
                        "/alarm/**", "/acquisitions/lost112", "/members/token/refresh", "/favicon.ico", "/members/after-login",
                                "/acquisitions/lost112/total-page", "/acquisitions/returns/count", "/location/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/members").permitAll()
                        .requestMatchers(HttpMethod.GET, "/acquisitions", "/losts").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                );
        http
                .httpBasic(Customizer.withDefaults());
        http
                .addFilterBefore(jwtExceptionFilter, JwtFilter.class)
                .exceptionHandling(handler -> handler.authenticationEntryPoint(entryPoint));

        return http.build();

    }
}
