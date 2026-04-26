package com.stablepay.infrastructure.temporal;

import static com.stablepay.testutil.WorkflowFixtures.SOME_AMOUNT_INR;
import static com.stablepay.testutil.WorkflowFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.WorkflowFixtures.SOME_CLAIM_BASE_URL;
import static com.stablepay.testutil.WorkflowFixtures.SOME_CLAIM_TOKEN;
import static com.stablepay.testutil.WorkflowFixtures.SOME_DEPOSIT_TX_SIGNATURE;
import static com.stablepay.testutil.WorkflowFixtures.SOME_DESTINATION_ADDRESS;
import static com.stablepay.testutil.WorkflowFixtures.SOME_ESCROW_EXPIRY_TIMESTAMP;
import static com.stablepay.testutil.WorkflowFixtures.SOME_ESCROW_PDA;
import static com.stablepay.testutil.WorkflowFixtures.SOME_RECIPIENT_PHONE;
import static com.stablepay.testutil.WorkflowFixtures.SOME_REFUND_TX_SIGNATURE;
import static com.stablepay.testutil.WorkflowFixtures.SOME_RELEASE_TX_SIGNATURE;
import static com.stablepay.testutil.WorkflowFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.WorkflowFixtures.SOME_SENDER_ADDRESS;
import static com.stablepay.testutil.WorkflowFixtures.SOME_UPI_ID;
import static com.stablepay.testutil.WorkflowFixtures.workflowRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.stablepay.domain.remittance.exception.DisbursementException;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.model.TransactionConfirmationStatus;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;

class RemittanceLifecycleWorkflowImplTest {

    private static final String TASK_QUEUE = "test-remittance-lifecycle";

    private TestWorkflowEnvironment testEnv;
    private Worker worker;
    private WorkflowClient client;
    private RemittanceLifecycleActivities activities;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        worker = testEnv.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(RemittanceLifecycleWorkflowImpl.class);

        activities = mock(RemittanceLifecycleActivities.class);
        given(activities.checkTransactionStatus(SOME_DEPOSIT_TX_SIGNATURE))
                .willReturn(TransactionConfirmationStatus.FINALIZED);
        given(activities.checkTransactionStatus(SOME_RELEASE_TX_SIGNATURE))
                .willReturn(TransactionConfirmationStatus.FINALIZED);
        given(activities.checkTransactionStatus(SOME_REFUND_TX_SIGNATURE))
                .willReturn(TransactionConfirmationStatus.FINALIZED);
        given(activities.deriveEscrowPda(SOME_REMITTANCE_ID.toString()))
                .willReturn(SOME_ESCROW_PDA);
        worker.registerActivitiesImplementations(activities);

        client = testEnv.getWorkflowClient();
    }

    @AfterEach
    void tearDown() {
        testEnv.close();
    }

    @Test
    void shouldCompleteFullLifecycleFromDepositThroughClaimToDelivered() {
        // given
        var request = workflowRequestBuilder()
                .claimExpiryTimeout(Duration.ofHours(48))
                .build();

        given(activities.depositEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC,
                SOME_ESCROW_EXPIRY_TIMESTAMP))
                .willReturn(SOME_DEPOSIT_TX_SIGNATURE);

        given(activities.releaseEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_DESTINATION_ADDRESS,
                SOME_SENDER_ADDRESS))
                .willReturn(SOME_RELEASE_TX_SIGNATURE);

        testEnv.start();

        var workflowId = RemittanceLifecycleWorkflow.workflowId(SOME_REMITTANCE_ID);
        var workflow = client.newWorkflowStub(
                RemittanceLifecycleWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .build());

        var claimSignal = ClaimSignal.builder()
                .claimToken(SOME_CLAIM_TOKEN)
                .upiId(SOME_UPI_ID)
                .destinationAddress(SOME_DESTINATION_ADDRESS)
                .build();

        testEnv.registerDelayedCallback(Duration.ofMinutes(1), () -> {
            var signalStub = client.newWorkflowStub(
                    RemittanceLifecycleWorkflow.class, workflowId);
            signalStub.claimSubmitted(claimSignal);
        });

        // when
        var result = workflow.execute(request);

        // then
        var expected = RemittanceWorkflowResult.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .finalStatus(RemittanceStatus.DELIVERED.name())
                .depositTxSignature(SOME_DEPOSIT_TX_SIGNATURE)
                .txSignature(SOME_RELEASE_TX_SIGNATURE)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        then(activities).should().depositEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC,
                SOME_ESCROW_EXPIRY_TIMESTAMP);

        then(activities).should().updateRemittanceStatusWithEscrowPda(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.ESCROWED, SOME_ESCROW_PDA);

        then(activities).should().sendClaimSms(
                SOME_RECIPIENT_PHONE,
                SOME_CLAIM_BASE_URL + SOME_CLAIM_TOKEN);

        then(activities).should().releaseEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_DESTINATION_ADDRESS,
                SOME_SENDER_ADDRESS);

        then(activities).should().updateRemittanceStatus(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.CLAIMED);

        then(activities).should().disburseInr(
                SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, SOME_REMITTANCE_ID.toString());

        then(activities).should().updateRemittanceStatus(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.DELIVERED);
    }

    @Test
    void shouldRefundWhenNoClaimReceivedWithinExpiryTimeout() {
        // given
        var request = workflowRequestBuilder()
                .claimExpiryTimeout(Duration.ofSeconds(1))
                .build();

        given(activities.depositEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC,
                SOME_ESCROW_EXPIRY_TIMESTAMP))
                .willReturn(SOME_DEPOSIT_TX_SIGNATURE);

        given(activities.refundEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS))
                .willReturn(SOME_REFUND_TX_SIGNATURE);

        testEnv.start();

        var workflow = client.newWorkflowStub(
                RemittanceLifecycleWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        // when
        var result = workflow.execute(request);

        // then
        var expected = RemittanceWorkflowResult.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .finalStatus(RemittanceStatus.REFUNDED.name())
                .depositTxSignature(SOME_DEPOSIT_TX_SIGNATURE)
                .txSignature(SOME_REFUND_TX_SIGNATURE)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        then(activities).should().depositEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC,
                SOME_ESCROW_EXPIRY_TIMESTAMP);

        then(activities).should().updateRemittanceStatusWithEscrowPda(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.ESCROWED, SOME_ESCROW_PDA);

        then(activities).should().refundEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS);

        then(activities).should().updateRemittanceStatus(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.REFUNDED);

        then(activities).should(never()).releaseEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_DESTINATION_ADDRESS,
                SOME_SENDER_ADDRESS);
    }

    @Test
    void shouldContinueWorkflowWhenSmsNotificationFails() {
        // given
        var request = workflowRequestBuilder()
                .claimExpiryTimeout(Duration.ofSeconds(1))
                .build();

        given(activities.depositEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC,
                SOME_ESCROW_EXPIRY_TIMESTAMP))
                .willReturn(SOME_DEPOSIT_TX_SIGNATURE);

        given(activities.refundEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS))
                .willReturn(SOME_REFUND_TX_SIGNATURE);

        willThrow(new RuntimeException("SMS delivery failed"))
                .given(activities).sendClaimSms(
                        SOME_RECIPIENT_PHONE,
                        SOME_CLAIM_BASE_URL + SOME_CLAIM_TOKEN);

        testEnv.start();

        var workflow = client.newWorkflowStub(
                RemittanceLifecycleWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        // when
        var result = workflow.execute(request);

        // then
        var expected = RemittanceWorkflowResult.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .finalStatus(RemittanceStatus.REFUNDED.name())
                .depositTxSignature(SOME_DEPOSIT_TX_SIGNATURE)
                .txSignature(SOME_REFUND_TX_SIGNATURE)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        then(activities).should().updateRemittanceStatusWithEscrowPda(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.ESCROWED, SOME_ESCROW_PDA);
    }

    @Test
    void shouldSetSmsFailureFlagAndStillCompleteWhenClaimReceived() {
        // given
        var request = workflowRequestBuilder()
                .claimExpiryTimeout(Duration.ofHours(48))
                .build();

        given(activities.depositEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC,
                SOME_ESCROW_EXPIRY_TIMESTAMP))
                .willReturn(SOME_DEPOSIT_TX_SIGNATURE);

        willThrow(new RuntimeException("SMS delivery failed"))
                .given(activities).sendClaimSms(
                        SOME_RECIPIENT_PHONE,
                        SOME_CLAIM_BASE_URL + SOME_CLAIM_TOKEN);

        given(activities.releaseEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_DESTINATION_ADDRESS,
                SOME_SENDER_ADDRESS))
                .willReturn(SOME_RELEASE_TX_SIGNATURE);

        testEnv.start();

        var workflowId = "sms-fail-claim-" + SOME_REMITTANCE_ID;
        var workflow = client.newWorkflowStub(
                RemittanceLifecycleWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .build());

        var claimSignal = ClaimSignal.builder()
                .claimToken(SOME_CLAIM_TOKEN)
                .upiId(SOME_UPI_ID)
                .destinationAddress(SOME_DESTINATION_ADDRESS)
                .build();

        testEnv.registerDelayedCallback(Duration.ofMinutes(1), () -> {
            var signalStub = client.newWorkflowStub(
                    RemittanceLifecycleWorkflow.class, workflowId);
            signalStub.claimSubmitted(claimSignal);
        });

        // when
        var result = workflow.execute(request);

        // then
        var expected = RemittanceWorkflowResult.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .finalStatus(RemittanceStatus.DELIVERED.name())
                .depositTxSignature(SOME_DEPOSIT_TX_SIGNATURE)
                .txSignature(SOME_RELEASE_TX_SIGNATURE)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldReturnCurrentStatusViaQueryDuringWorkflow() {
        // given
        var request = workflowRequestBuilder()
                .claimExpiryTimeout(Duration.ofHours(48))
                .build();

        given(activities.depositEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC,
                SOME_ESCROW_EXPIRY_TIMESTAMP))
                .willReturn(SOME_DEPOSIT_TX_SIGNATURE);

        given(activities.releaseEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_DESTINATION_ADDRESS,
                SOME_SENDER_ADDRESS))
                .willReturn(SOME_RELEASE_TX_SIGNATURE);

        testEnv.start();

        var workflowId = "query-status-" + SOME_REMITTANCE_ID;
        var workflow = client.newWorkflowStub(
                RemittanceLifecycleWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .build());

        var statusHolder = new CompletableFuture<RemittanceWorkflowStatus>();

        testEnv.registerDelayedCallback(Duration.ofHours(1), () -> {
            var queryStub = client.newWorkflowStub(
                    RemittanceLifecycleWorkflow.class, workflowId);
            statusHolder.complete(queryStub.getStatus());

            var claimSignal = ClaimSignal.builder()
                    .claimToken(SOME_CLAIM_TOKEN)
                    .upiId(SOME_UPI_ID)
                    .destinationAddress(SOME_DESTINATION_ADDRESS)
                    .build();
            queryStub.claimSubmitted(claimSignal);
        });

        // when
        workflow.execute(request);

        var status = statusHolder.join();

        // then
        var expected = RemittanceWorkflowStatus.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .currentStatus(RemittanceStatus.ESCROWED.name())
                .depositTxSignature(SOME_DEPOSIT_TX_SIGNATURE)
                .smsNotificationFailed(false)
                .build();

        assertThat(status)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldTransitionToDisbursementFailedWhenDisbursementThrows() {
        // given
        var request = workflowRequestBuilder()
                .claimExpiryTimeout(Duration.ofHours(48))
                .build();

        given(activities.depositEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC,
                SOME_ESCROW_EXPIRY_TIMESTAMP))
                .willReturn(SOME_DEPOSIT_TX_SIGNATURE);

        given(activities.releaseEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_DESTINATION_ADDRESS,
                SOME_SENDER_ADDRESS))
                .willReturn(SOME_RELEASE_TX_SIGNATURE);

        willThrow(DisbursementException.nonRetriable(SOME_UPI_ID, "invalid UPI"))
                .given(activities).disburseInr(
                        SOME_UPI_ID,
                        SOME_AMOUNT_USDC,
                        SOME_AMOUNT_INR,
                        SOME_REMITTANCE_ID.toString());

        testEnv.start();

        var workflowId = "disbursement-fail-" + SOME_REMITTANCE_ID;
        var workflow = client.newWorkflowStub(
                RemittanceLifecycleWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .build());

        var claimSignal = ClaimSignal.builder()
                .claimToken(SOME_CLAIM_TOKEN)
                .upiId(SOME_UPI_ID)
                .destinationAddress(SOME_DESTINATION_ADDRESS)
                .build();

        testEnv.registerDelayedCallback(Duration.ofMinutes(1), () -> {
            var signalStub = client.newWorkflowStub(
                    RemittanceLifecycleWorkflow.class, workflowId);
            signalStub.claimSubmitted(claimSignal);
        });

        // when
        var result = workflow.execute(request);

        // then
        var expected = RemittanceWorkflowResult.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .finalStatus(RemittanceStatus.DISBURSEMENT_FAILED.name())
                .depositTxSignature(SOME_DEPOSIT_TX_SIGNATURE)
                .txSignature(SOME_RELEASE_TX_SIGNATURE)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        then(activities).should().releaseEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_DESTINATION_ADDRESS,
                SOME_SENDER_ADDRESS);

        then(activities).should().updateRemittanceStatus(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.CLAIMED);

        then(activities).should().updateRemittanceStatus(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.DISBURSEMENT_FAILED);

        then(activities).should(never()).updateRemittanceStatus(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.DELIVERED);
    }

    @Test
    void shouldProcessClaimReceivedJustBeforeTimeout() {
        // given
        var request = workflowRequestBuilder()
                .claimExpiryTimeout(Duration.ofHours(48))
                .build();

        given(activities.depositEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC,
                SOME_ESCROW_EXPIRY_TIMESTAMP))
                .willReturn(SOME_DEPOSIT_TX_SIGNATURE);

        given(activities.releaseEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_DESTINATION_ADDRESS,
                SOME_SENDER_ADDRESS))
                .willReturn(SOME_RELEASE_TX_SIGNATURE);

        testEnv.start();

        var workflowId = "claim-before-timeout-" + SOME_REMITTANCE_ID;
        var workflow = client.newWorkflowStub(
                RemittanceLifecycleWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .build());

        var claimSignal = ClaimSignal.builder()
                .claimToken(SOME_CLAIM_TOKEN)
                .upiId(SOME_UPI_ID)
                .destinationAddress(SOME_DESTINATION_ADDRESS)
                .build();

        testEnv.registerDelayedCallback(Duration.ofHours(47), () -> {
            var signalStub = client.newWorkflowStub(
                    RemittanceLifecycleWorkflow.class, workflowId);
            signalStub.claimSubmitted(claimSignal);
        });

        // when
        var result = workflow.execute(request);

        // then
        var expected = RemittanceWorkflowResult.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .finalStatus(RemittanceStatus.DELIVERED.name())
                .depositTxSignature(SOME_DEPOSIT_TX_SIGNATURE)
                .txSignature(SOME_RELEASE_TX_SIGNATURE)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        then(activities).should(never()).refundEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS);
    }

    @Test
    void shouldConfigureDisbursementActivityWithRetryOptionsExcludingNonRetriable() {
        // given
        var expected = RetryOptions.newBuilder()
                .setMaximumAttempts(3)
                .setInitialInterval(Duration.ofSeconds(2))
                .setBackoffCoefficient(2.0)
                .setDoNotRetry(
                        "com.stablepay.domain.remittance.exception.DisbursementException$NonRetriable")
                .build();

        // when
        var retryOptions = RemittanceLifecycleWorkflowImpl.disbursementActivityOptions().getRetryOptions();

        // then
        assertThat(retryOptions)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldRetryTransientDisbursementFailureAndReachDelivered() {
        // given
        var request = workflowRequestBuilder()
                .claimExpiryTimeout(Duration.ofHours(48))
                .build();

        given(activities.depositEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC,
                SOME_ESCROW_EXPIRY_TIMESTAMP))
                .willReturn(SOME_DEPOSIT_TX_SIGNATURE);

        given(activities.releaseEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_DESTINATION_ADDRESS,
                SOME_SENDER_ADDRESS))
                .willReturn(SOME_RELEASE_TX_SIGNATURE);

        var attempts = new AtomicInteger(0);
        given(activities.disburseInr(
                SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, SOME_REMITTANCE_ID.toString()))
                .willAnswer(invocation -> {
                    if (attempts.incrementAndGet() < 3) {
                        throw DisbursementException.retriable(
                                SOME_UPI_ID, new RuntimeException("Razorpay 5xx"));
                    }
                    return null;
                });

        testEnv.start();

        var workflowId = "disbursement-retry-" + SOME_REMITTANCE_ID;
        var workflow = client.newWorkflowStub(
                RemittanceLifecycleWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .build());

        var claimSignal = ClaimSignal.builder()
                .claimToken(SOME_CLAIM_TOKEN)
                .upiId(SOME_UPI_ID)
                .destinationAddress(SOME_DESTINATION_ADDRESS)
                .build();

        testEnv.registerDelayedCallback(Duration.ofMinutes(1), () -> {
            var signalStub = client.newWorkflowStub(
                    RemittanceLifecycleWorkflow.class, workflowId);
            signalStub.claimSubmitted(claimSignal);
        });

        // when
        var result = workflow.execute(request);

        // then
        var expected = RemittanceWorkflowResult.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .finalStatus(RemittanceStatus.DELIVERED.name())
                .depositTxSignature(SOME_DEPOSIT_TX_SIGNATURE)
                .txSignature(SOME_RELEASE_TX_SIGNATURE)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        assertThat(attempts.get()).isEqualTo(3);

        then(activities).should().updateRemittanceStatus(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.DELIVERED);

        then(activities).should(never()).updateRemittanceStatus(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.DISBURSEMENT_FAILED);
    }

    @Test
    void shouldNotRetryWhenDisbursementExceptionIsNonRetriable() {
        // given
        var request = workflowRequestBuilder()
                .claimExpiryTimeout(Duration.ofHours(48))
                .build();

        given(activities.depositEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC,
                SOME_ESCROW_EXPIRY_TIMESTAMP))
                .willReturn(SOME_DEPOSIT_TX_SIGNATURE);

        given(activities.releaseEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_DESTINATION_ADDRESS,
                SOME_SENDER_ADDRESS))
                .willReturn(SOME_RELEASE_TX_SIGNATURE);

        var attempts = new AtomicInteger(0);
        given(activities.disburseInr(
                SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, SOME_REMITTANCE_ID.toString()))
                .willAnswer(invocation -> {
                    attempts.incrementAndGet();
                    throw DisbursementException.nonRetriable(SOME_UPI_ID, "invalid UPI");
                });

        testEnv.start();

        var workflowId = "disbursement-non-retriable-" + SOME_REMITTANCE_ID;
        var workflow = client.newWorkflowStub(
                RemittanceLifecycleWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .build());

        var claimSignal = ClaimSignal.builder()
                .claimToken(SOME_CLAIM_TOKEN)
                .upiId(SOME_UPI_ID)
                .destinationAddress(SOME_DESTINATION_ADDRESS)
                .build();

        testEnv.registerDelayedCallback(Duration.ofMinutes(1), () -> {
            var signalStub = client.newWorkflowStub(
                    RemittanceLifecycleWorkflow.class, workflowId);
            signalStub.claimSubmitted(claimSignal);
        });

        // when
        var result = workflow.execute(request);

        // then
        var expected = RemittanceWorkflowResult.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .finalStatus(RemittanceStatus.DISBURSEMENT_FAILED.name())
                .depositTxSignature(SOME_DEPOSIT_TX_SIGNATURE)
                .txSignature(SOME_RELEASE_TX_SIGNATURE)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        assertThat(attempts.get()).isEqualTo(1);

        then(activities).should().updateRemittanceStatus(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.DISBURSEMENT_FAILED);

        then(activities).should(never()).updateRemittanceStatus(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.DELIVERED);
    }

    @Nested
    class TransactionConfirmation {

        @Test
        void shouldTransitionToDepositFailedWhenConfirmationTimesOut() {
        // given
        var request = workflowRequestBuilder()
                .claimExpiryTimeout(Duration.ofHours(48))
                .build();

        given(activities.depositEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC,
                SOME_ESCROW_EXPIRY_TIMESTAMP))
                .willReturn(SOME_DEPOSIT_TX_SIGNATURE);

        given(activities.checkTransactionStatus(SOME_DEPOSIT_TX_SIGNATURE))
                .willReturn(TransactionConfirmationStatus.NOT_FOUND);

        testEnv.start();

        var workflow = client.newWorkflowStub(
                RemittanceLifecycleWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        // when
        var result = workflow.execute(request);

        // then
        var expected = RemittanceWorkflowResult.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .finalStatus(RemittanceStatus.DEPOSIT_FAILED.name())
                .depositTxSignature(SOME_DEPOSIT_TX_SIGNATURE)
                .txSignature(SOME_DEPOSIT_TX_SIGNATURE)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        then(activities).should().updateRemittanceStatus(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.DEPOSIT_FAILED);

        then(activities).should(never()).updateRemittanceStatusWithEscrowPda(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.ESCROWED, SOME_ESCROW_PDA);
    }

    @Test
    void shouldTransitionToDepositFailedWhenTransactionFailsOnChain() {
        // given
        var request = workflowRequestBuilder()
                .claimExpiryTimeout(Duration.ofHours(48))
                .build();

        given(activities.depositEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC,
                SOME_ESCROW_EXPIRY_TIMESTAMP))
                .willReturn(SOME_DEPOSIT_TX_SIGNATURE);

        given(activities.checkTransactionStatus(SOME_DEPOSIT_TX_SIGNATURE))
                .willReturn(TransactionConfirmationStatus.FAILED_ON_CHAIN);

        testEnv.start();

        var workflow = client.newWorkflowStub(
                RemittanceLifecycleWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        // when
        var result = workflow.execute(request);

        // then
        var expected = RemittanceWorkflowResult.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .finalStatus(RemittanceStatus.DEPOSIT_FAILED.name())
                .depositTxSignature(SOME_DEPOSIT_TX_SIGNATURE)
                .txSignature(SOME_DEPOSIT_TX_SIGNATURE)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        then(activities).should().updateRemittanceStatus(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.DEPOSIT_FAILED);
    }

    @Test
    void shouldTransitionToClaimFailedWhenReleaseConfirmationFails() {
        // given
        var request = workflowRequestBuilder()
                .claimExpiryTimeout(Duration.ofHours(48))
                .build();

        given(activities.depositEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC,
                SOME_ESCROW_EXPIRY_TIMESTAMP))
                .willReturn(SOME_DEPOSIT_TX_SIGNATURE);

        given(activities.releaseEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_DESTINATION_ADDRESS,
                SOME_SENDER_ADDRESS))
                .willReturn(SOME_RELEASE_TX_SIGNATURE);

        given(activities.checkTransactionStatus(SOME_RELEASE_TX_SIGNATURE))
                .willReturn(TransactionConfirmationStatus.FAILED_ON_CHAIN);

        testEnv.start();

        var workflowId = "claim-fail-" + SOME_REMITTANCE_ID;
        var workflow = client.newWorkflowStub(
                RemittanceLifecycleWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .build());

        var claimSignal = ClaimSignal.builder()
                .claimToken(SOME_CLAIM_TOKEN)
                .upiId(SOME_UPI_ID)
                .destinationAddress(SOME_DESTINATION_ADDRESS)
                .build();

        testEnv.registerDelayedCallback(Duration.ofMinutes(1), () -> {
            var signalStub = client.newWorkflowStub(
                    RemittanceLifecycleWorkflow.class, workflowId);
            signalStub.claimSubmitted(claimSignal);
        });

        // when
        var result = workflow.execute(request);

        // then
        var expected = RemittanceWorkflowResult.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .finalStatus(RemittanceStatus.CLAIM_FAILED.name())
                .depositTxSignature(SOME_DEPOSIT_TX_SIGNATURE)
                .txSignature(SOME_RELEASE_TX_SIGNATURE)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        then(activities).should().updateRemittanceStatus(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.CLAIM_FAILED);

        then(activities).should(never()).disburseInr(
                SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, SOME_REMITTANCE_ID.toString());
    }

    @Test
    void shouldTransitionToRefundFailedWhenRefundConfirmationFails() {
        // given
        var request = workflowRequestBuilder()
                .claimExpiryTimeout(Duration.ofSeconds(1))
                .build();

        given(activities.depositEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC,
                SOME_ESCROW_EXPIRY_TIMESTAMP))
                .willReturn(SOME_DEPOSIT_TX_SIGNATURE);

        given(activities.refundEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS))
                .willReturn(SOME_REFUND_TX_SIGNATURE);

        given(activities.checkTransactionStatus(SOME_REFUND_TX_SIGNATURE))
                .willReturn(TransactionConfirmationStatus.FAILED_ON_CHAIN);

        testEnv.start();

        var workflow = client.newWorkflowStub(
                RemittanceLifecycleWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        // when
        var result = workflow.execute(request);

        // then
        var expected = RemittanceWorkflowResult.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .finalStatus(RemittanceStatus.REFUND_FAILED.name())
                .depositTxSignature(SOME_DEPOSIT_TX_SIGNATURE)
                .txSignature(SOME_REFUND_TX_SIGNATURE)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        then(activities).should().updateRemittanceStatus(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.REFUND_FAILED);

        then(activities).should(never()).updateRemittanceStatus(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.REFUNDED);
    }

    @Test
    void shouldConfirmDepositBeforeMarkingEscrowed() {
        // given
        var request = workflowRequestBuilder()
                .claimExpiryTimeout(Duration.ofSeconds(1))
                .build();

        given(activities.depositEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS,
                SOME_AMOUNT_USDC,
                SOME_ESCROW_EXPIRY_TIMESTAMP))
                .willReturn(SOME_DEPOSIT_TX_SIGNATURE);

        var attempts = new AtomicInteger(0);
        given(activities.checkTransactionStatus(SOME_DEPOSIT_TX_SIGNATURE))
                .willAnswer(invocation -> {
                    if (attempts.incrementAndGet() < 3) {
                        return TransactionConfirmationStatus.PROCESSED;
                    }
                    return TransactionConfirmationStatus.FINALIZED;
                });

        given(activities.refundEscrow(
                SOME_REMITTANCE_ID.toString(),
                SOME_SENDER_ADDRESS))
                .willReturn(SOME_REFUND_TX_SIGNATURE);

        testEnv.start();

        var workflow = client.newWorkflowStub(
                RemittanceLifecycleWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        // when
        var result = workflow.execute(request);

        // then
        var expected = RemittanceWorkflowResult.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .finalStatus(RemittanceStatus.REFUNDED.name())
                .depositTxSignature(SOME_DEPOSIT_TX_SIGNATURE)
                .txSignature(SOME_REFUND_TX_SIGNATURE)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
        assertThat(attempts.get()).isGreaterThanOrEqualTo(3);

        then(activities).should().updateRemittanceStatusWithEscrowPda(
                SOME_REMITTANCE_ID.toString(), RemittanceStatus.ESCROWED, SOME_ESCROW_PDA);
    }
    }
}
