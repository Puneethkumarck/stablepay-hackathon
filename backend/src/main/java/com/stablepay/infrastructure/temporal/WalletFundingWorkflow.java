package com.stablepay.infrastructure.temporal;

import java.util.UUID;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface WalletFundingWorkflow {

    String WORKFLOW_ID_PREFIX = "wallet-funding-";

    static String workflowId(UUID fundingId) {
        return WORKFLOW_ID_PREFIX + fundingId;
    }

    @WorkflowMethod
    void execute(WalletFundingWorkflowRequest request);
}
