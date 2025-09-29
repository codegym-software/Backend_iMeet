package com.example.iMeetBE.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.example.iMeetBE.repository")
public class JpaConfig {
}
