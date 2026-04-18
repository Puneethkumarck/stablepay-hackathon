package com.stablepay.infrastructure.temporal;

import static com.stablepay.testutil.WorkflowFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.WorkflowFixtures.SOME_CLAIM_TOKEN;
import static com.stablepay.testutil.WorkflowFixtures.SOME_RECIPIENT_PHONE;
import static com.stablepay.testutil.WorkflowFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.WorkflowFixtures.SOME_SENDER_ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.application.config.StablePayProperties;
import com.stablepay.application.config.StablePayProperties.TemporalProperties;

@ExtendWith(MockitoExtension.class)
class TemporalRemittanceWorkflowStarterTest {

    @Mock
    private WorkflowFactory workflowFactory;

    @Mock
    private StablePayProperties properties;

    @Mock
    private RemittanceLifecycleWorkflow workflowStub;

    @Captor
    private ArgumentCaptor<RemittanceWorkflowRequest> requestCaptor;

    @InjectMocks
    private TemporalRemittanceWorkflowStarter starter;

    @Test
    void shouldCreateWorkflowViaFactoryAndStart() {
        // given
        var temporalProperties = new TemporalProperties(
                Duration.ofHours(48),
                Duration.ofHours(2),
                Duration.ofHours(48),
                "https://claim.stablepay.app/");
        given(properties.temporal()).willReturn(temporalProperties);
        given(workflowFactory.createRemittanceWorkflow(SOME_REMITTANCE_ID))
                .willReturn(workflowStub);

        // when
        starter.startWorkflow(
                SOME_REMITTANCE_ID,
                SOME_SENDER_ADDRESS,
                SOME_RECIPIENT_PHONE,
                SOME_AMOUNT_USDC,
                SOME_CLAIM_TOKEN);

        // then
        then(workflowFactory).should().createRemittanceWorkflow(SOME_REMITTANCE_ID);
        then(workflowStub).should().execute(requestCaptor.capture());

        var expected = RemittanceWorkflowRequest.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .senderAddress(SOME_SENDER_ADDRESS)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .amountInr(BigDecimal.ZERO)
                .claimToken(SOME_CLAIM_TOKEN)
                .claimBaseUrl("https://claim.stablepay.app/")
                .claimExpiryTimeout(Duration.ofHours(48))
                .escrowExpiryTimestamp(0L)
                .build();

        assertThat(requestCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("escrowExpiryTimestamp")
                .isEqualTo(expected);

        assertThat(requestCaptor.getValue().escrowExpiryTimestamp())
                .isPositive();
    }
}
