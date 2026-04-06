package com.stablepay.test;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.stablepay.infrastructure.temporal.RemittanceLifecycleActivities;
import com.stablepay.infrastructure.temporal.RemittanceLifecycleWorkflowImpl;
import com.stablepay.infrastructure.temporal.TaskQueue;

import io.temporal.client.WorkflowClient;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;

@TestConfiguration
public class TemporalTestConfig {

    @Bean
    TestWorkflowEnvironment testWorkflowEnvironment() {
        return TestWorkflowEnvironment.newInstance();
    }

    @Bean
    WorkflowClient workflowClient(TestWorkflowEnvironment testEnv) {
        return testEnv.getWorkflowClient();
    }

    @Bean
    @Primary
    RemittanceLifecycleActivities remittanceLifecycleActivities() {
        return Mockito.mock(RemittanceLifecycleActivities.class);
    }

    @Bean
    Worker worker(
            TestWorkflowEnvironment testEnv,
            RemittanceLifecycleActivities activities) {
        var worker = testEnv.newWorker(TaskQueue.REMITTANCE_LIFECYCLE.getName());
        worker.registerWorkflowImplementationTypes(RemittanceLifecycleWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        testEnv.start();
        return worker;
    }
}
