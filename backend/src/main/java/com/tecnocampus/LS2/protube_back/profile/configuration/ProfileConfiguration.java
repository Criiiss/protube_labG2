package com.tecnocampus.LS2.protube_back.profile.configuration;

import com.tecnocampus.LS2.protube_back.profile.domain.service.ProfileValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProfileConfiguration {
    @Bean
    public ProfileValidator profileValidator() {
        return new ProfileValidator();
    }
}
