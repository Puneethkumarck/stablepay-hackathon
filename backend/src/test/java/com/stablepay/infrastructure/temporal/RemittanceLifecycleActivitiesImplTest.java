package com.stablepay.infrastructure.temporal;

import static com.stablepay.testutil.RemittanceFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.WorkflowFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.WorkflowFixtures.SOME_DEPOSIT_TX_SIGNATURE;
import static com.stablepay.testutil.WorkflowFixtures.SOME_DESTINATION_ADDRESS;
import static com.stablepay.testutil.WorkflowFixtures.SOME_ESCROW_EXPIRY_TIMESTAMP;
import static com.stablepay.testutil.WorkflowFixtures.SOME_RECIPIENT_PHONE;
import static com.stablepay.testutil.WorkflowFixtures.SOME_REFUND_TX_SIGNATURE;
import static com.stablepay.testutil.WorkflowFixtures.SOME_RELEASE_TX_SIGNATURE;
import static com.stablepay.testutil.WorkflowFixtures.SOME_SENDER_ADDRESS;
import static com.stablepay.testutil.WorkflowFixtures.SOME_UPI_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.common.port.SmsProvider;
import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.remittance.handler.UpdateRemittanceStatusHandler;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.port.FiatDisbursementProvider;
import com.stablepay.domain.remittance.port.SolanaTransactionService;

@ExtendWith(MockitoExtension.class)
class RemittanceLifecycleActivitiesImplTest {

    private static final String SOME_CLAIM_URL = "https://claim.stablepay.app/claim-token-abc-123";
    private static final BigDecimal SOME_DISBURSEMENT_AMOUNT = new BigDecimal("100.00");

    @Mock
    private SolanaTransactionService solanaTransactionService;

    @Mock
    private SmsProvider smsProvider;

    @Mock
    private FiatDisbursementProvider fiatDisbursementProvider;

    @Mock
    private UpdateRemittanceStatusHandler updateRemittanceStatusHandler;

    @InjectMocks
    private RemittanceLifecycleActivitiesImpl activities;

    @Test
    void shouldDepositEscrowViaSolanaService() {
        // given
        given(solanaTransactionService.depositEscrow(
                SOME_REMITTANCE_ID, SOME_SENDER_ADDRESS, SOME_AMOUNT_USDC, SOME_ESCROW_EXPIRY_TIMESTAMP))
                .willReturn(SOME_DEPOSIT_TX_SIGNATURE);

        // when
        var result = activities.depositEscrow(
                SOME_REMITTANCE_ID.toString(), SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC, SOME_ESCROW_EXPIRY_TIMESTAMP);

        // then
        assertThat(result).isEqualTo(SOME_DEPOSIT_TX_SIGNATURE);
    }

    @Test
    void shouldReleaseEscrowViaSolanaService() {
        // given
        given(solanaTransactionService.claimEscrow(SOME_REMITTANCE_ID, SOME_DESTINATION_ADDRESS, SOME_SENDER_ADDRESS))
                .willReturn(SOME_RELEASE_TX_SIGNATURE);

        // when
        var result = activities.releaseEscrow(SOME_REMITTANCE_ID.toString(), SOME_DESTINATION_ADDRESS, SOME_SENDER_ADDRESS);

        // then
        assertThat(result).isEqualTo(SOME_RELEASE_TX_SIGNATURE);
    }

    @Test
    void shouldRefundEscrowViaSolanaService() {
        // given
        given(solanaTransactionService.refundEscrow(SOME_REMITTANCE_ID, SOME_SENDER_ADDRESS))
                .willReturn(SOME_REFUND_TX_SIGNATURE);

        // when
        var result = activities.refundEscrow(SOME_REMITTANCE_ID.toString(), SOME_SENDER_ADDRESS);

        // then
        assertThat(result).isEqualTo(SOME_REFUND_TX_SIGNATURE);
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
    void shouldDelegateInrDisbursementToProvider() {
        // given — provider is void, no stubbing needed

        // when
        activities.disburseInr(SOME_UPI_ID, SOME_DISBURSEMENT_AMOUNT, SOME_REMITTANCE_ID.toString());

        // then
        then(fiatDisbursementProvider).should().disburse(SOME_UPI_ID, SOME_DISBURSEMENT_AMOUNT, SOME_REMITTANCE_ID.toString());
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
