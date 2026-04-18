package com.stablepay.infrastructure.temporal;

import static com.stablepay.infrastructure.temporal.TaskQueue.Constants.TASK_QUEUE_WALLET_FUNDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import com.stablepay.domain.wallet.exception.TreasuryDepletedException;
import com.stablepay.test.TemporalTest;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;

@TemporalTest
class WalletFundingWorkflowIntegrationTest {

    private static final String SOME_SENDER_ADDRESS = "WfItSend3r1234567890AbCdEfGhIjKlMnOp";
    private static final BigDecimal SOME_AMOUNT_USDC = new BigDecimal("25.00");
    private static final String SOME_TX_SIGNATURE = "WfItSig1234567890AbCdEfGhIjKlMnOpQrStUv";

    @Autowired
    private WorkflowClient workflowClient;

    @Autowired
    private WalletFundingActivities activities;

    private UUID fundingId;
    private Long walletId;

    @BeforeEach
    void setUp() {
        fundingId = UUID.randomUUID();
        walletId = 42L;
        Mockito.reset(activities);
    }

    private WalletFundingWorkflow newWorkflow() {
        return workflowClient.newWorkflowStub(
                WalletFundingWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE_WALLET_FUNDING)
                        .setWorkflowId(WalletFundingWorkflow.workflowId(fundingId))
                        .build());
    }

    private WalletFundingWorkflowRequest newRequest() {
        return WalletFundingWorkflowRequest.builder()
                .fundingId(fundingId)
                .walletId(walletId)
                .senderSolanaAddress(SOME_SENDER_ADDRESS)
                .amountUsdc(SOME_AMOUNT_USDC)
                .build();
    }

    @Test
    void shouldExecuteFiveActivitiesInOrderOnWalletFundingQueue() {
        // given
        given(activities.transferUsdc(SOME_SENDER_ADDRESS, SOME_AMOUNT_USDC))
                .willReturn(SOME_TX_SIGNATURE);

        // when
        newWorkflow().execute(newRequest());

        // then
        var inOrder = inOrder(activities);
        inOrder.verify(activities).checkTreasuryBalance(SOME_AMOUNT_USDC);
        inOrder.verify(activities).ensureSolBalance(SOME_SENDER_ADDRESS);
        inOrder.verify(activities).createAtaIfNeeded(SOME_SENDER_ADDRESS);
        inOrder.verify(activities).transferUsdc(SOME_SENDER_ADDRESS, SOME_AMOUNT_USDC);
        inOrder.verify(activities).finalizeFunding(fundingId, walletId, SOME_AMOUNT_USDC);
    }

    @Test
    void shouldFailWorkflowAndSkipDownstreamActivitiesWhenTreasuryIsEmpty() {
        // given
        willThrow(TreasuryDepletedException.insufficientTreasury(SOME_AMOUNT_USDC, BigDecimal.ZERO))
                .given(activities).checkTreasuryBalance(SOME_AMOUNT_USDC);

        // when / then
        assertThatThrownBy(() -> newWorkflow().execute(newRequest()))
                .isInstanceOf(WorkflowFailedException.class);

        then(activities).should().checkTreasuryBalance(SOME_AMOUNT_USDC);
        then(activities).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldUseCorrectTaskQueueForWalletFundingWorkflow() {
        // given
        given(activities.transferUsdc(SOME_SENDER_ADDRESS, SOME_AMOUNT_USDC))
                .willReturn(SOME_TX_SIGNATURE);

        // when
        newWorkflow().execute(newRequest());

        // then
        assertThat(TASK_QUEUE_WALLET_FUNDING).isEqualTo("stablepay-wallet-funding");
    }
}
