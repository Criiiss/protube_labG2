package com.tecnocampus.LS2.protube_back.profile.configuration;

import com.tecnocampus.LS2.protube_back.profile.domain.service.ProfileValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ProfileConfiguration {
    @Bean
    public ProfileValidator profileValidator() {
        return new ProfileValidator();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
