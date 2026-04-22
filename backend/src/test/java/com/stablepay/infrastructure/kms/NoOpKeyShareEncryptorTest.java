package com.stablepay.infrastructure.kms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stablepay.domain.wallet.model.DecryptedKeyMaterial;
import com.stablepay.domain.wallet.model.EncryptedKeyMaterial;

@ExtendWith(MockitoExtension.class)
class NoOpKeyShareEncryptorTest {

    private static final byte[] KEY_SHARE = {1, 2, 3, 4, 5};
    private static final byte[] PEER_KEY_SHARE = {6, 7, 8, 9, 10};

    @Mock
    private JdbcTemplate jdbcTemplate;

    private NoOpKeyShareEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new NoOpKeyShareEncryptor(jdbcTemplate);
    }

    @Test
    void shouldReturnKeySharesUnchangedOnEncrypt() {
        // given
        var expected = EncryptedKeyMaterial.builder()
                .encryptedKeyShareData(KEY_SHARE)
                .encryptedPeerKeyShareData(PEER_KEY_SHARE)
                .encryptedDek(null)
                .keyShareIv(null)
                .peerKeyShareIv(null)
                .build();

        // when
        var result = encryptor.encrypt(KEY_SHARE, PEER_KEY_SHARE);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnKeySharesUnchangedOnDecrypt() {
        // given
        var expected = DecryptedKeyMaterial.builder()
                .keyShareData(KEY_SHARE)
                .peerKeyShareData(PEER_KEY_SHARE)
                .build();

        // when
        var result = encryptor.decrypt(KEY_SHARE, PEER_KEY_SHARE, null, null, null);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldFailValidationWhenEncryptedWalletsExist() {
        // given
        given(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM wallets WHERE key_share_dek IS NOT NULL", Long.class))
                .willReturn(3L);

        // when / then
        assertThatThrownBy(() -> encryptor.validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("3 wallet(s) have encrypted key shares");
    }

    @Test
    void shouldPassValidationWhenNoEncryptedWalletsExist() {
        // given
        given(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM wallets WHERE key_share_dek IS NOT NULL", Long.class))
                .willReturn(0L);

        // when / then — no exception
        encryptor.validate();
    }
}
