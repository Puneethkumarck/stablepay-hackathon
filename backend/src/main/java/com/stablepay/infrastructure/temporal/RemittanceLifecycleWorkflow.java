package com.stablepay.infrastructure.temporal;

import java.util.UUID;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface RemittanceLifecycleWorkflow {

    String WORKFLOW_ID_PREFIX = "stablepay-remittance-";

    static String workflowId(UUID remittanceId) {
        return WORKFLOW_ID_PREFIX + remittanceId;
    }

    @WorkflowMethod
    RemittanceWorkflowResult execute(RemittanceWorkflowRequest request);

    @SignalMethod
    void claimSubmitted(ClaimSignal claimSignal);

    @QueryMethod
    RemittanceWorkflowStatus getStatus();
}
