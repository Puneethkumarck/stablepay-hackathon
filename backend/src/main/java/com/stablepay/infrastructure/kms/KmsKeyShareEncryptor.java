package com.stablepay.infrastructure.kms;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.stablepay.domain.wallet.exception.KeyShareEncryptionException;
import com.stablepay.domain.wallet.model.DecryptedKeyMaterial;
import com.stablepay.domain.wallet.model.EncryptedKeyMaterial;
import com.stablepay.domain.wallet.port.KeyShareEncryptor;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stablepay.kms.enabled", havingValue = "true")
public class KmsKeyShareEncryptor implements KeyShareEncryptor {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final Map<String, String> ENCRYPTION_CONTEXT = Map.of(
            "purpose", "wallet-key-share"
    );

    private final KmsClient kmsClient;
    private final KmsProperties kmsProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public EncryptedKeyMaterial encrypt(byte[] keyShareData, byte[] peerKeyShareData) {
        var generateRequest = GenerateDataKeyRequest.builder()
                .keyId(kmsProperties.keyArn())
                .keySpec(DataKeySpec.AES_256)
                .encryptionContext(ENCRYPTION_CONTEXT)
                .build();
        var dataKeyResponse = kmsClient.generateDataKey(generateRequest);

        byte[] plaintextDek = dataKeyResponse.plaintext().asByteArray();
        byte[] encryptedDek = dataKeyResponse.ciphertextBlob().asByteArray();

        try {
            byte[] keyShareIv = generateIv();
            byte[] encryptedKeyShare = aesGcmEncrypt(keyShareData, plaintextDek, keyShareIv);

            byte[] peerKeyShareIv = generateIv();
            byte[] encryptedPeerKeyShare = aesGcmEncrypt(peerKeyShareData, plaintextDek, peerKeyShareIv);

            return EncryptedKeyMaterial.builder()
                    .encryptedKeyShareData(encryptedKeyShare)
                    .encryptedPeerKeyShareData(encryptedPeerKeyShare)
                    .encryptedDek(encryptedDek)
                    .keyShareIv(keyShareIv)
                    .peerKeyShareIv(peerKeyShareIv)
                    .build();
        } finally {
            Arrays.fill(plaintextDek, (byte) 0);
        }
    }

    @Override
    public DecryptedKeyMaterial decrypt(byte[] encKeyShare, byte[] encPeerKeyShare,
                                         byte[] encDek, byte[] iv, byte[] peerIv) {
        var decryptRequest = DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(encDek))
                .encryptionContext(ENCRYPTION_CONTEXT)
                .build();
        byte[] plaintextDek = kmsClient.decrypt(decryptRequest).plaintext().asByteArray();

        try {
            byte[] keyShareData = aesGcmDecrypt(encKeyShare, plaintextDek, iv);
            byte[] peerKeyShareData = aesGcmDecrypt(encPeerKeyShare, plaintextDek, peerIv);

            return DecryptedKeyMaterial.builder()
                    .keyShareData(keyShareData)
                    .peerKeyShareData(peerKeyShareData)
                    .build();
        } finally {
            Arrays.fill(plaintextDek, (byte) 0);
        }
    }

    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        return iv;
    }

    private static byte[] aesGcmEncrypt(byte[] plaintext, byte[] key, byte[] iv) {
        try {
            var cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return cipher.doFinal(plaintext);
        } catch (Exception e) {
            throw new KeyShareEncryptionException("Failed to encrypt key share", e);
        }
    }

    private static byte[] aesGcmDecrypt(byte[] ciphertext, byte[] key, byte[] iv) {
        try {
            var cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new KeyShareEncryptionException("Failed to decrypt key share", e);
        }
    }
}
