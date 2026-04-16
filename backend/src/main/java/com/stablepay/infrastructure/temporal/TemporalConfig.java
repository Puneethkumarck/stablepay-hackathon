package com.stablepay.infrastructure.temporal;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.stablepay.application.config.StablePayProperties;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.common.converter.JacksonJsonPayloadConverter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableConfigurationProperties(StablePayProperties.class)
public class TemporalConfig {

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
    @Profile("!test")
    WorkflowServiceStubs workflowServiceStubs() {
        var target = System.getenv().getOrDefault("TEMPORAL_ADDRESS",
                System.getenv().getOrDefault("TEMPORAL_FRONTEND_URL", "127.0.0.1:7233"));
        var options = WorkflowServiceStubsOptions.newBuilder()
                .setTarget(target)
                .build();
        log.info("Connecting to Temporal server at {}", target);
        return WorkflowServiceStubs.newServiceStubs(options);
    }

    @Bean
    @Profile("!test")
    WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs, DataConverter dataConverter) {
        var namespace = System.getenv().getOrDefault("TEMPORAL_NAMESPACE", "default");
        var options = WorkflowClientOptions.newBuilder()
                .setNamespace(namespace)
                .setDataConverter(dataConverter)
                .build();
        log.info("Creating Temporal WorkflowClient for namespace {}", namespace);
        return WorkflowClient.newInstance(serviceStubs, options);
    }

    @Bean
    @Profile("!test")
    WorkerFactory workerFactory(WorkflowClient workflowClient) {
        return WorkerFactory.newInstance(workflowClient);
    }

    @Bean
    @Profile("!test")
    Worker worker(
            WorkerFactory workerFactory,
            ObjectProvider<RemittanceLifecycleActivities> activitiesProvider) {
        var taskQueue = TaskQueue.REMITTANCE_LIFECYCLE.getName();
        log.info("Creating Temporal worker for task queue {}", taskQueue);
        var worker = workerFactory.newWorker(taskQueue);
        worker.registerWorkflowImplementationTypes(RemittanceLifecycleWorkflowImpl.class);
        activitiesProvider.ifAvailable(worker::registerActivitiesImplementations);
        workerFactory.start();
        log.info("Temporal WorkerFactory started, polling task queue {}", taskQueue);
        return worker;
    }
}
