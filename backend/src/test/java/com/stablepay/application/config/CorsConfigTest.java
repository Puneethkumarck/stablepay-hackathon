package com.stablepay.application.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stablepay.testutil.TestClockConfig;

import lombok.SneakyThrows;

@WebMvcTest(CorsConfigTest.TestController.class)
@Import({CorsConfig.class, CorsConfigTest.CorsTestProperties.class, TestClockConfig.class})
class CorsConfigTest {

    private static final String ALLOWED_ORIGIN_WEB_CLAIM = "http://localhost:3000";
    private static final String ALLOWED_ORIGIN_EXPO = "http://localhost:8081";
    private static final String DISALLOWED_ORIGIN = "http://evil.example.com";
    private static final String API_ENDPOINT = "/api/test";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint;

    @Test
    @SneakyThrows
    void shouldReturnCorsHeadersOnPreflightRequest() {
        // given
        var origin = ALLOWED_ORIGIN_WEB_CLAIM;

        // when / then
        mockMvc.perform(options(API_ENDPOINT)
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_MAX_AGE));
    }

    @Test
    @SneakyThrows
    void shouldReturnCorsHeadersOnActualRequest() {
        // given
        var origin = ALLOWED_ORIGIN_WEB_CLAIM;

        // when / then
        mockMvc.perform(get(API_ENDPOINT)
                        .with(jwt())
                        .header(HttpHeaders.ORIGIN, origin))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
    }

    @Test
    @SneakyThrows
    void shouldRejectDisallowedOriginOnPreflightRequest() {
        // given
        var origin = DISALLOWED_ORIGIN;

        // when / then
        mockMvc.perform(options(API_ENDPOINT)
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @SneakyThrows
    void shouldAllowExpoDevOriginOnPreflightRequest() {
        // given
        var origin = ALLOWED_ORIGIN_EXPO;

        // when / then
        mockMvc.perform(options(API_ENDPOINT)
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
    }

    @Test
    @SneakyThrows
    void shouldNotApplyCorsToNonApiPaths() {
        // given
        var origin = ALLOWED_ORIGIN_WEB_CLAIM;

        // when / then
        mockMvc.perform(options("/health")
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @RestController
    @RequestMapping("/api/test")
    static class TestController {

        @GetMapping
        public String test() {
            return "ok";
        }
    }

    @TestConfiguration
    static class CorsTestProperties {

        @Bean
        public CorsProperties corsProperties() {
            return CorsProperties.builder()
                    .allowedOrigins(List.of("http://localhost:3000", "http://localhost:8081"))
                    .allowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"))
                    .allowedHeaders(List.of("Content-Type", "Authorization"))
                    .maxAge(3600L)
                    .build();
        }
    }
}
