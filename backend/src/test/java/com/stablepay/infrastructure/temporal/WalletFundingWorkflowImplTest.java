package com.stablepay.infrastructure.temporal;

import static com.stablepay.testutil.WalletFundingFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.WalletFundingFixtures.SOME_FUNDING_ID;
import static com.stablepay.testutil.WalletFundingFixtures.SOME_SENDER_ADDRESS;
import static com.stablepay.testutil.WalletFundingFixtures.SOME_TX_SIGNATURE;
import static com.stablepay.testutil.WalletFundingFixtures.SOME_WALLET_ID;
import static com.stablepay.testutil.WalletFundingFixtures.requestBuilder;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;

import com.stablepay.domain.wallet.exception.TreasuryDepletedException;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;

class WalletFundingWorkflowImplTest {

    private static final String TASK_QUEUE = "test-wallet-funding";

    private TestWorkflowEnvironment testEnv;
    private Worker worker;
    private WorkflowClient client;
    private WalletFundingActivities activities;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        worker = testEnv.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(WalletFundingWorkflowImpl.class);

        activities = mock(WalletFundingActivities.class);
        worker.registerActivitiesImplementations(activities);

        client = testEnv.getWorkflowClient();
    }

    @AfterEach
    void tearDown() {
        testEnv.close();
    }

    private WalletFundingWorkflow newWorkflow() {
        return client.newWorkflowStub(
                WalletFundingWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(WalletFundingWorkflow.workflowId(SOME_FUNDING_ID))
                        .build());
    }

    @Test
    void shouldExecuteFiveActivitiesInOrder() {
        // given
        given(activities.transferUsdc(SOME_SENDER_ADDRESS, SOME_AMOUNT_USDC))
                .willReturn(SOME_TX_SIGNATURE);
        testEnv.start();

        // when
        newWorkflow().execute(requestBuilder().build());

        // then
        var inOrder = BDDMockito.inOrder(activities);
        then(activities).should(inOrder).checkTreasuryBalance(SOME_AMOUNT_USDC);
        then(activities).should(inOrder).ensureSolBalance(SOME_SENDER_ADDRESS);
        then(activities).should(inOrder).createAtaIfNeeded(SOME_SENDER_ADDRESS);
        then(activities).should(inOrder).transferUsdc(SOME_SENDER_ADDRESS, SOME_AMOUNT_USDC);
        then(activities).should(inOrder).finalizeFunding(SOME_FUNDING_ID, SOME_WALLET_ID, SOME_AMOUNT_USDC);
    }

    @Test
    void shouldFailWorkflowWhenTreasuryInsufficient() {
        // given
        willThrow(TreasuryDepletedException.insufficientTreasury(SOME_AMOUNT_USDC, BigDecimal.ZERO))
                .given(activities).checkTreasuryBalance(SOME_AMOUNT_USDC);
        testEnv.start();

        // when / then
        assertThatThrownBy(() -> newWorkflow().execute(requestBuilder().build()))
                .isInstanceOf(WorkflowFailedException.class)
                .hasCauseInstanceOf(ActivityFailure.class);

        then(activities).should(never()).ensureSolBalance(SOME_SENDER_ADDRESS);
        then(activities).should(never()).createAtaIfNeeded(SOME_SENDER_ADDRESS);
        then(activities).should(never()).transferUsdc(SOME_SENDER_ADDRESS, SOME_AMOUNT_USDC);
        then(activities).should(never()).finalizeFunding(SOME_FUNDING_ID, SOME_WALLET_ID, SOME_AMOUNT_USDC);
    }
}
