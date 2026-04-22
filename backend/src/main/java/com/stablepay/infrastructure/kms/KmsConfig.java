package com.stablepay.infrastructure.kms;

import java.net.URI;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "stablepay.kms.enabled", havingValue = "true")
@EnableConfigurationProperties(KmsProperties.class)
@RequiredArgsConstructor
public class KmsConfig {

    private final KmsProperties kmsProperties;
    private final Environment environment;

    @PostConstruct
    void validateEndpoint() {
        if (environment.matchesProfiles("production") && StringUtils.hasText(kmsProperties.endpoint())) {
            throw new IllegalStateException("KMS endpoint override is not allowed in production profile");
        }
    }

    @Bean
    public KmsClient kmsClient() {
        var builder = KmsClient.builder()
                .region(Region.of(kmsProperties.region() != null ? kmsProperties.region() : "us-east-1"));

        if (StringUtils.hasText(kmsProperties.endpoint())) {
            log.info("Using custom KMS endpoint: {}", kmsProperties.endpoint());
            builder.endpointOverride(URI.create(kmsProperties.endpoint()));
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test", "test")));
        }

        log.info("KMS client initialized for key ARN: {}...{}",
                maskArn(kmsProperties.keyArn()), "");
        return builder.build();
    }

    private static String maskArn(String arn) {
        if (arn == null || arn.length() <= 20) {
            return "****";
        }
        return arn.substring(0, 16) + "****";
    }
}
