package com.stablepay.infrastructure.temporal;

import static com.stablepay.infrastructure.temporal.TaskQueue.Constants.TASK_QUEUE_REMITTANCE_LIFECYCLE;
import static com.stablepay.testutil.WorkflowFixtures.SOME_AMOUNT_INR;
import static com.stablepay.testutil.WorkflowFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.WorkflowFixtures.SOME_DESTINATION_ADDRESS;
import static com.stablepay.testutil.WorkflowFixtures.SOME_SENDER_ADDRESS;
import static com.stablepay.testutil.WorkflowFixtures.SOME_UPI_ID;
import static com.stablepay.testutil.WorkflowFixtures.claimSignalBuilder;
import static com.stablepay.testutil.WorkflowFixtures.workflowRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import com.stablepay.domain.remittance.exception.DisbursementException;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.test.TemporalTest;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;

@TemporalTest
class RemittanceLifecycleWorkflowIntegrationTest {

    @Autowired
    private WorkflowClient workflowClient;

    @Autowired
    private TestWorkflowEnvironment testEnv;

    @Autowired
    private RemittanceLifecycleActivities activities;

    private UUID remittanceId;

    @BeforeEach
    void setUp() {
        remittanceId = UUID.randomUUID();
        Mockito.reset(activities);
    }

    private RemittanceLifecycleWorkflow startWorkflow() {
        var workflow = workflowClient.newWorkflowStub(
                RemittanceLifecycleWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE_REMITTANCE_LIFECYCLE)
                        .setWorkflowId(
                                RemittanceLifecycleWorkflow.workflowId(remittanceId))
                        .build());

        var request = workflowRequestBuilder()
                .remittanceId(remittanceId)
                .build();
        WorkflowClient.start(workflow::execute, request);
        return workflow;
    }

    @Nested
    class HappyPath {

        @Test
        void shouldDeliverWhenClaimReceivedBeforeTimeout() {
            // given
            var workflow = startWorkflow();

            // when
            workflow.claimSubmitted(claimSignalBuilder().build());

            var result = WorkflowStub.fromTyped(workflow)
                    .getResult(RemittanceWorkflowResult.class);

            // then
            var expected = RemittanceWorkflowResult.builder()
                    .remittanceId(remittanceId)
                    .finalStatus(RemittanceStatus.DELIVERED.name())
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("escrowPda", "txSignature")
                    .isEqualTo(expected);

            then(activities)
                    .should()
                    .updateRemittanceStatus(
                            remittanceId.toString(), RemittanceStatus.ESCROWED);
            then(activities)
                    .should()
                    .updateRemittanceStatus(
                            remittanceId.toString(), RemittanceStatus.CLAIMED);
            then(activities)
                    .should()
                    .disburseInr(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, remittanceId.toString());
            then(activities)
                    .should()
                    .updateRemittanceStatus(
                            remittanceId.toString(), RemittanceStatus.DELIVERED);
        }
    }

    @Nested
    class Timeout {

        @Test
        void shouldRefundWhenClaimTimesOut() {
            // given
            var workflow = startWorkflow();

            // when
            testEnv.sleep(Duration.ofHours(49));

            var result = WorkflowStub.fromTyped(workflow)
                    .getResult(RemittanceWorkflowResult.class);

            // then
            var expected = RemittanceWorkflowResult.builder()
                    .remittanceId(remittanceId)
                    .finalStatus(RemittanceStatus.REFUNDED.name())
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("escrowPda", "txSignature")
                    .isEqualTo(expected);

            then(activities)
                    .should()
                    .updateRemittanceStatus(
                            remittanceId.toString(), RemittanceStatus.ESCROWED);
            then(activities)
                    .should()
                    .updateRemittanceStatus(
                            remittanceId.toString(), RemittanceStatus.REFUNDED);
            then(activities)
                    .should(never())
                    .updateRemittanceStatus(
                            remittanceId.toString(), RemittanceStatus.DELIVERED);
        }
    }

    @Nested
    class SmsFailure {

        @Test
        void shouldContinueWorkflowWhenSmsDeliveryFails() {
            // given
            var request = workflowRequestBuilder().remittanceId(remittanceId).build();
            var expectedClaimUrl = request.claimBaseUrl() + request.claimToken();
            willThrow(new RuntimeException("Twilio error"))
                    .given(activities)
                    .sendClaimSms(request.recipientPhone(), expectedClaimUrl);

            var workflow = startWorkflow();

            // when
            workflow.claimSubmitted(claimSignalBuilder().build());

            var result = WorkflowStub.fromTyped(workflow)
                    .getResult(RemittanceWorkflowResult.class);

            // then
            var expected = RemittanceWorkflowResult.builder()
                    .remittanceId(remittanceId)
                    .finalStatus(RemittanceStatus.DELIVERED.name())
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("escrowPda", "txSignature")
                    .isEqualTo(expected);
        }
    }

    @Nested
    class DisbursementFailure {

        @Test
        void shouldTransitionToDisbursementFailedWhenDisbursementThrows() {
            // given
            willThrow(DisbursementException.nonRetriable(SOME_UPI_ID, "invalid UPI"))
                    .given(activities)
                    .disburseInr(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, remittanceId.toString());

            var workflow = startWorkflow();

            // when
            workflow.claimSubmitted(claimSignalBuilder().build());

            var result = WorkflowStub.fromTyped(workflow)
                    .getResult(RemittanceWorkflowResult.class);

            // then
            var expected = RemittanceWorkflowResult.builder()
                    .remittanceId(remittanceId)
                    .finalStatus(RemittanceStatus.DISBURSEMENT_FAILED.name())
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("escrowPda", "txSignature")
                    .isEqualTo(expected);

            then(activities)
                    .should()
                    .releaseEscrow(
                            remittanceId.toString(),
                            SOME_DESTINATION_ADDRESS,
                            SOME_SENDER_ADDRESS);
            then(activities)
                    .should()
                    .updateRemittanceStatus(
                            remittanceId.toString(), RemittanceStatus.CLAIMED);
            then(activities)
                    .should()
                    .updateRemittanceStatus(
                            remittanceId.toString(), RemittanceStatus.DISBURSEMENT_FAILED);
            then(activities)
                    .should(never())
                    .updateRemittanceStatus(
                            remittanceId.toString(), RemittanceStatus.DELIVERED);
        }
    }

    @Nested
    class QueryStatus {

        @Test
        void shouldReturnCurrentStatusViaQuery() {
            // given
            var workflow = startWorkflow();
            testEnv.sleep(Duration.ofSeconds(1));

            // when
            var status = workflow.getStatus();

            // then
            var expected = RemittanceWorkflowStatus.builder()
                    .remittanceId(remittanceId)
                    .currentStatus(RemittanceStatus.ESCROWED.name())
                    .smsNotificationFailed(false)
                    .build();

            assertThat(status)
                    .usingRecursiveComparison()
                    .ignoringFields("escrowPda")
                    .isEqualTo(expected);
        }
    }
}
