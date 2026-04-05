package com.stablepay.infrastructure.temporal;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.stablepay.application.config.StablePayProperties;
import com.stablepay.application.config.StablePayProperties.TemporalProperties.ActivityConfig;

import io.temporal.activity.ActivityOptions;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.common.converter.JacksonJsonPayloadConverter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableConfigurationProperties(StablePayProperties.class)
@ConditionalOnProperty(
        prefix = "stablepay.temporal",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class TemporalConfig {

    @Bean
    WorkflowServiceStubs workflowServiceStubs(StablePayProperties properties) {
        var temporal = properties.temporal();
        var options = WorkflowServiceStubsOptions.newBuilder()
                .setTarget(temporal.target())
                .build();
        log.info("Connecting to Temporal server at {}", temporal.target());
        return WorkflowServiceStubs.newServiceStubs(options);
    }

    @Bean
    DataConverter dataConverter() {
        var objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
                .build();
        return DefaultDataConverter.newDefaultInstance()
                .withPayloadConverterOverrides(new JacksonJsonPayloadConverter(objectMapper));
    }

    @Bean
    WorkflowClient workflowClient(
            WorkflowServiceStubs serviceStubs,
            StablePayProperties properties,
            DataConverter dataConverter) {
        var temporal = properties.temporal();
        var options = WorkflowClientOptions.newBuilder()
                .setNamespace(temporal.namespace())
                .setDataConverter(dataConverter)
                .build();
        log.info("Creating Temporal WorkflowClient for namespace {}", temporal.namespace());
        return WorkflowClient.newInstance(serviceStubs, options);
    }

    @Bean
    WorkerFactory workerFactory(WorkflowClient workflowClient) {
        return WorkerFactory.newInstance(workflowClient);
    }

    @Bean
    WorkflowImplementationOptions workflowImplementationOptions(StablePayProperties properties) {
        var temporal = properties.temporal();
        var activityOptions = temporal.activityOptions();

        var activityMethodOptions = Map.of(
                "signEscrowDeposit",
                        buildActivityOptions(activityOptions.signing()),
                "signEscrowRelease",
                        buildActivityOptions(activityOptions.signing()),
                "submitToSolana",
                        buildActivityOptions(activityOptions.solanaSubmission()),
                "sendClaimSms",
                        buildActivityOptions(activityOptions.smsDelivery()));

        return WorkflowImplementationOptions.newBuilder()
                .setDefaultActivityOptions(buildActivityOptions(activityOptions.defaultOptions()))
                .setActivityOptions(activityMethodOptions)
                .build();
    }

    @Bean
    Worker worker(
            WorkerFactory workerFactory,
            StablePayProperties properties,
            WorkflowImplementationOptions workflowImplOptions,
            ObjectProvider<RemittanceLifecycleActivities> activitiesProvider) {
        var temporal = properties.temporal();
        log.info("Creating Temporal worker for task queue {}", temporal.taskQueue());
        var worker = workerFactory.newWorker(temporal.taskQueue());
        worker.registerWorkflowImplementationTypes(
                workflowImplOptions, RemittanceLifecycleWorkflowImpl.class);
        activitiesProvider.ifAvailable(worker::registerActivitiesImplementations);
        return worker;
    }

    @Bean
    WorkflowOptions workflowOptions(StablePayProperties properties) {
        var temporal = properties.temporal();
        return WorkflowOptions.newBuilder()
                .setTaskQueue(temporal.taskQueue())
                .setWorkflowExecutionTimeout(temporal.workflowExecutionTimeout())
                .setWorkflowRunTimeout(temporal.workflowRunTimeout())
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE_FAILED_ONLY)
                .build();
    }

    private ActivityOptions buildActivityOptions(ActivityConfig config) {
        var retryOptions = RetryOptions.newBuilder()
                .setMaximumAttempts(config.maxAttempts())
                .setInitialInterval(config.initialInterval())
                .setBackoffCoefficient(config.backoffCoefficient())
                .build();

        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(config.startToCloseTimeout())
                .setRetryOptions(retryOptions)
                .build();
    }
}
