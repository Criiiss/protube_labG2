package com.tecnocampus.LS2.protube_back.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

public class MvcConfigTest {
    @Test
    void sendsDatesAsIsoStringsInsteadOfNumbers() throws Exception {
        var json = new MappingJackson2HttpMessageConverter();
        List<HttpMessageConverter<?>> converters = new ArrayList<>(List.of(json));

        new MvcConfig().extendMessageConverters(converters);

        String body = json.getObjectMapper().writeValueAsString(Map.of(
                "instant", Instant.parse("2026-11-27T10:15:30Z"),
                "offset", OffsetDateTime.parse("2026-11-27T11:15:30+01:00")));
        assertThat(body).contains("\"2026-11-27T10:15:30Z\"").contains("\"2026-11-27T11:15:30+01:00\"");
    }
}
