package com.stablepay.infrastructure.temporal;

import static com.stablepay.testutil.MpcFixtures.SOME_SIGNATURE;
import static com.stablepay.testutil.MpcFixtures.SOME_TRANSACTION_BYTES;
import static com.stablepay.testutil.RemittanceFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.WorkflowFixtures.SOME_RECIPIENT_PHONE;
import static com.stablepay.testutil.WorkflowFixtures.SOME_UPI_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.common.port.SmsProvider;
import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.remittance.handler.UpdateRemittanceStatusHandler;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.wallet.port.MpcWalletClient;

@ExtendWith(MockitoExtension.class)
class RemittanceLifecycleActivitiesImplTest {

    private static final byte[] EMPTY_KEY_SHARE = new byte[0];
    private static final String SOME_CLAIM_URL = "https://claim.stablepay.app/claim-token-abc-123";
    private static final String SOME_AMOUNT_INR = "8325.00";

    @Mock
    private MpcWalletClient mpcWalletClient;

    @Mock
    private SmsProvider smsProvider;

    @Mock
    private UpdateRemittanceStatusHandler updateRemittanceStatusHandler;

    @InjectMocks
    private RemittanceLifecycleActivitiesImpl activities;

    @Test
    void shouldSignEscrowDepositViaMpc() {
        // given
        given(mpcWalletClient.signTransaction(SOME_TRANSACTION_BYTES, EMPTY_KEY_SHARE))
                .willReturn(SOME_SIGNATURE);

        // when
        var result = activities.signEscrowDeposit(SOME_TRANSACTION_BYTES);

        // then
        assertThat(result).isEqualTo(SOME_SIGNATURE);
    }

    @Test
    void shouldSignEscrowReleaseViaMpc() {
        // given
        given(mpcWalletClient.signTransaction(SOME_TRANSACTION_BYTES, EMPTY_KEY_SHARE))
                .willReturn(SOME_SIGNATURE);

        // when
        var result = activities.signEscrowRelease(SOME_TRANSACTION_BYTES);

        // then
        assertThat(result).isEqualTo(SOME_SIGNATURE);
    }

    @Test
    void shouldSubmitToSolanaAndReturnSignature() {
        // given
        var signedTxBytes = new byte[]{1, 2, 3, 4, 5};

        // when
        var result = activities.submitToSolana(signedTxBytes);

        // then
        assertThat(result).startsWith("sim_").isNotBlank();
    }

    @Test
    void shouldSendClaimSmsViaProvider() {
        // given — no stubbing needed, sendSms returns void

        // when
        activities.sendClaimSms(SOME_RECIPIENT_PHONE, SOME_CLAIM_URL);

        // then
        var expectedMessage = "You have a StablePay remittance! Claim your funds: " + SOME_CLAIM_URL;
        then(smsProvider).should().sendSms(SOME_RECIPIENT_PHONE, expectedMessage);
    }

    @Test
    void shouldSimulateInrDisbursementWithoutError() {
        // given — no dependencies to stub

        // when
        activities.simulateInrDisbursement(SOME_UPI_ID, SOME_AMOUNT_INR);

        // then
        then(smsProvider).shouldHaveNoInteractions();
        then(mpcWalletClient).shouldHaveNoInteractions();
    }

    @Test
    void shouldUpdateRemittanceStatusViaDomainHandler() {
        // given — handler is void, no stubbing needed

        // when
        activities.updateRemittanceStatus(SOME_REMITTANCE_ID.toString(), RemittanceStatus.ESCROWED);

        // then
        then(updateRemittanceStatusHandler).should().handle(SOME_REMITTANCE_ID, RemittanceStatus.ESCROWED);
    }

    @Test
    void shouldPropagateExceptionWhenRemittanceNotFoundForStatusUpdate() {
        // given
        willThrow(RemittanceNotFoundException.byId(SOME_REMITTANCE_ID))
                .given(updateRemittanceStatusHandler).handle(SOME_REMITTANCE_ID, RemittanceStatus.ESCROWED);

        // when / then
        assertThatThrownBy(() -> activities.updateRemittanceStatus(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.ESCROWED))
                .isInstanceOf(RemittanceNotFoundException.class)
                .hasMessageContaining("SP-0010")
                .hasMessageContaining(SOME_REMITTANCE_ID.toString());
    }
}
