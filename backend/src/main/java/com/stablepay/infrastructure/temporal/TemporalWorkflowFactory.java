package com.stablepay.infrastructure.temporal;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.stablepay.application.config.StablePayProperties;

import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(WorkflowClient.class)
public class TemporalWorkflowFactory implements WorkflowFactory {

    private final WorkflowClient workflowClient;
    private final StablePayProperties properties;

    @Override
    public RemittanceLifecycleWorkflow createRemittanceWorkflow(UUID remittanceId) {
        var temporal = properties.temporal();
        return workflowClient.newWorkflowStub(
                RemittanceLifecycleWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TaskQueue.REMITTANCE_LIFECYCLE.getName())
                        .setWorkflowId(RemittanceLifecycleWorkflow.workflowId(remittanceId))
                        .setWorkflowExecutionTimeout(temporal.workflowExecutionTimeout())
                        .setWorkflowRunTimeout(temporal.workflowRunTimeout())
                        .setWorkflowIdReusePolicy(
                                WorkflowIdReusePolicy
                                        .WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE_FAILED_ONLY)
                        .build());
    }

    @Override
    public boolean isWorkflowRunning(String workflowId) {
        var namespace = workflowClient.getOptions().getNamespace();
        var request = DescribeWorkflowExecutionRequest.newBuilder()
                .setNamespace(namespace)
                .setExecution(
                        io.temporal.api.common.v1.WorkflowExecution.newBuilder()
                                .setWorkflowId(workflowId)
                                .build())
                .build();
        try {
            var response = workflowClient
                    .getWorkflowServiceStubs()
                    .blockingStub()
                    .describeWorkflowExecution(request);
            return response.getWorkflowExecutionInfo().getStatus()
                    == io.temporal.api.enums.v1.WorkflowExecutionStatus
                            .WORKFLOW_EXECUTION_STATUS_RUNNING;
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Code.NOT_FOUND) {
                return false;
            }
            throw e;
        } catch (WorkflowNotFoundException e) {
            return false;
        }
    }
}
