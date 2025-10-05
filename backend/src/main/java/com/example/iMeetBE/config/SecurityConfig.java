package com.example.iMeetBE.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.iMeetBE.security.JwtAuthenticationFilter;
import com.example.iMeetBE.service.CustomOAuth2UserService;
import com.example.iMeetBE.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
//
    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private CustomOAuth2AuthorizationRequestResolver customOAuth2AuthorizationRequestResolver;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(request -> {
                var corsConfiguration = new org.springframework.web.cors.CorsConfiguration();
                corsConfiguration.setAllowedOriginPatterns(java.util.List.of("*"));
                corsConfiguration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                corsConfiguration.setAllowedHeaders(java.util.List.of("*"));
                corsConfiguration.setAllowCredentials(false);
                return corsConfiguration;
            }))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Allow sessions for OAuth2
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/forgot-password/**").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/signup").permitAll()
                .requestMatchers("/api/auth/check-auth", "/api/auth/change-password").permitAll()
                .requestMatchers("/api/auth/upload-avatar", "/api/auth/remove-avatar").authenticated()
                .requestMatchers("/api/users/profile").authenticated()
                .requestMatchers("/api/rooms/**").permitAll() // Cho phép test API rooms mà không cần authentication
                .requestMatchers("/api/oauth2/**").permitAll()
                .requestMatchers("/api/cognito/**").hasRole("ADMIN")
                .requestMatchers("/api/test/**").permitAll()
                .requestMatchers("/api/aws/**").permitAll()
                .requestMatchers("/login/oauth2/**").permitAll()
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/oauth2/authorization/cognito")
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestResolver(customOAuth2AuthorizationRequestResolver)
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oauth2AuthenticationSuccessHandler())
                .failureUrl("/login?error=oauth2_error")
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler() {
        SimpleUrlAuthenticationSuccessHandler handler = new SimpleUrlAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl("http://localhost:3000/oauth2/callback"); // Always redirect to callback
        handler.setAlwaysUseDefaultTargetUrl(true);
        return handler;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}