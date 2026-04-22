package com.stablepay.test;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.KeySpec;
import software.amazon.awssdk.services.kms.model.KeyUsageType;

public class LocalStackKmsContainerExtension implements BeforeAllCallback {

    private static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.4"))
                    .withServices(LocalStackContainer.Service.KMS);

    private static String keyArn;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!LOCALSTACK.isRunning()) {
            LOCALSTACK.start();
            keyArn = createKmsKey();
        }
    }

    public static String getKeyArn() {
        return keyArn;
    }

    public static String getEndpoint() {
        return LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.KMS).toString();
    }

    public static String getRegion() {
        return LOCALSTACK.getRegion();
    }

    private String createKmsKey() {
        var endpoint = LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.KMS);
        try (var kmsClient = KmsClient.builder()
                .endpointOverride(endpoint)
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .build()) {
            var response = kmsClient.createKey(CreateKeyRequest.builder()
                    .keySpec(KeySpec.SYMMETRIC_DEFAULT)
                    .keyUsage(KeyUsageType.ENCRYPT_DECRYPT)
                    .build());
            return response.keyMetadata().arn();
        }
    }
}
