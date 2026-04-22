package com.stablepay.infrastructure.kms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

@ExtendWith(MockitoExtension.class)
class KmsKeyShareEncryptorTest {

    private static final String KEY_ARN = "arn:aws:kms:us-east-1:000000000000:key/test-key-id";
    private static final byte[] KEY_SHARE = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    private static final byte[] PEER_KEY_SHARE = {11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
    private static final byte[] PLAINTEXT_DEK = new byte[32];
    private static final byte[] ENCRYPTED_DEK = {99, 98, 97, 96, 95};

    static {
        for (int i = 0; i < PLAINTEXT_DEK.length; i++) {
            PLAINTEXT_DEK[i] = (byte) (i + 1);
        }
    }

    @Mock
    private KmsClient kmsClient;

    @Mock
    private KmsProperties kmsProperties;

    @InjectMocks
    private KmsKeyShareEncryptor encryptor;

    @Test
    void shouldEncryptAndDecryptKeySharesRoundTrip() {
        // given
        var generateRequest = GenerateDataKeyRequest.builder()
                .keyId(KEY_ARN)
                .keySpec(DataKeySpec.AES_256)
                .build();
        var generateResponse = GenerateDataKeyResponse.builder()
                .plaintext(SdkBytes.fromByteArray(PLAINTEXT_DEK.clone()))
                .ciphertextBlob(SdkBytes.fromByteArray(ENCRYPTED_DEK))
                .build();
        given(kmsProperties.keyArn()).willReturn(KEY_ARN);
        given(kmsClient.generateDataKey(generateRequest)).willReturn(generateResponse);

        // when
        var encrypted = encryptor.encrypt(KEY_SHARE, PEER_KEY_SHARE);

        // then
        assertThat(encrypted.encryptedKeyShareData()).isNotEqualTo(KEY_SHARE);
        assertThat(encrypted.encryptedPeerKeyShareData()).isNotEqualTo(PEER_KEY_SHARE);
        assertThat(encrypted.encryptedDek()).isEqualTo(ENCRYPTED_DEK);

        // given — decrypt
        var decryptRequest = DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(ENCRYPTED_DEK))
                .build();
        var decryptResponse = DecryptResponse.builder()
                .plaintext(SdkBytes.fromByteArray(PLAINTEXT_DEK.clone()))
                .build();
        given(kmsClient.decrypt(decryptRequest)).willReturn(decryptResponse);

        // when
        var decrypted = encryptor.decrypt(
                encrypted.encryptedKeyShareData(), encrypted.encryptedPeerKeyShareData(),
                encrypted.encryptedDek(), encrypted.keyShareIv(), encrypted.peerKeyShareIv());

        // then
        assertThat(decrypted.keyShareData()).isEqualTo(KEY_SHARE);
        assertThat(decrypted.peerKeyShareData()).isEqualTo(PEER_KEY_SHARE);
    }

    @Test
    void shouldGenerateUniqueIvsPerShare() {
        // given
        var generateRequest = GenerateDataKeyRequest.builder()
                .keyId(KEY_ARN)
                .keySpec(DataKeySpec.AES_256)
                .build();
        var generateResponse = GenerateDataKeyResponse.builder()
                .plaintext(SdkBytes.fromByteArray(PLAINTEXT_DEK.clone()))
                .ciphertextBlob(SdkBytes.fromByteArray(ENCRYPTED_DEK))
                .build();
        given(kmsProperties.keyArn()).willReturn(KEY_ARN);
        given(kmsClient.generateDataKey(generateRequest)).willReturn(generateResponse);

        // when
        var encrypted = encryptor.encrypt(KEY_SHARE, PEER_KEY_SHARE);

        // then
        assertThat(encrypted.keyShareIv()).hasSize(12);
        assertThat(encrypted.peerKeyShareIv()).hasSize(12);
        assertThat(encrypted.keyShareIv()).isNotEqualTo(encrypted.peerKeyShareIv());
    }

    @Test
    void shouldThrowOnDecryptWithTamperedCiphertext() {
        // given
        var generateRequest = GenerateDataKeyRequest.builder()
                .keyId(KEY_ARN)
                .keySpec(DataKeySpec.AES_256)
                .build();
        var generateResponse = GenerateDataKeyResponse.builder()
                .plaintext(SdkBytes.fromByteArray(PLAINTEXT_DEK.clone()))
                .ciphertextBlob(SdkBytes.fromByteArray(ENCRYPTED_DEK))
                .build();
        given(kmsProperties.keyArn()).willReturn(KEY_ARN);
        given(kmsClient.generateDataKey(generateRequest)).willReturn(generateResponse);

        var encrypted = encryptor.encrypt(KEY_SHARE, PEER_KEY_SHARE);

        byte[] tamperedCiphertext = encrypted.encryptedKeyShareData().clone();
        tamperedCiphertext[0] ^= 0xFF;

        var decryptRequest = DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(ENCRYPTED_DEK))
                .build();
        var decryptResponse = DecryptResponse.builder()
                .plaintext(SdkBytes.fromByteArray(PLAINTEXT_DEK.clone()))
                .build();
        given(kmsClient.decrypt(decryptRequest)).willReturn(decryptResponse);

        // when / then
        assertThatThrownBy(() -> encryptor.decrypt(
                tamperedCiphertext, encrypted.encryptedPeerKeyShareData(),
                encrypted.encryptedDek(), encrypted.keyShareIv(), encrypted.peerKeyShareIv()))
                .isInstanceOf(KeyShareEncryptionException.class)
                .hasMessageContaining("Failed to decrypt key share");
    }
}
