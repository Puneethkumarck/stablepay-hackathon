package com.stablepay.infrastructure.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.twilio.Twilio;
import com.twilio.http.TwilioRestClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "stablepay.twilio.enabled", havingValue = "true")
@EnableConfigurationProperties(TwilioProperties.class)
public class TwilioConfig {

    @Bean
    public TwilioRestClient twilioRestClient(TwilioProperties properties) {
        log.info("Initializing Twilio client for account {}", maskAccountSid(properties.accountSid()));
        Twilio.init(properties.accountSid(), properties.authToken());
        return new TwilioRestClient.Builder(properties.accountSid(), properties.authToken()).build();
    }

    @Bean
    public TwilioSmsAdapter twilioSmsAdapter(TwilioRestClient twilioRestClient, TwilioProperties properties) {
        return new TwilioSmsAdapter(twilioRestClient, properties);
    }

    private static String maskAccountSid(String sid) {
        if (sid == null || sid.length() <= 8) {
            return "****";
        }
        return sid.substring(0, 4) + "****" + sid.substring(sid.length() - 4);
    }
}
