package com.stablepay.infrastructure.temporal;

import java.util.UUID;

public interface WorkflowFactory {

    RemittanceLifecycleWorkflow createRemittanceWorkflow(UUID remittanceId);

    boolean isWorkflowRunning(String workflowId);
}
