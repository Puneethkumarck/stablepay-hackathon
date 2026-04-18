package com.stablepay.test;

import java.util.List;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.stablepay.infrastructure.temporal.RemittanceLifecycleActivities;
import com.stablepay.infrastructure.temporal.RemittanceLifecycleWorkflowImpl;
import com.stablepay.infrastructure.temporal.TaskQueue;
import com.stablepay.infrastructure.temporal.WalletFundingActivities;
import com.stablepay.infrastructure.temporal.WalletFundingWorkflowImpl;

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
    @Primary
    WalletFundingActivities walletFundingActivities() {
        return Mockito.mock(WalletFundingActivities.class);
    }

    @Bean
    Worker remittanceLifecycleWorker(
            TestWorkflowEnvironment testEnv,
            RemittanceLifecycleActivities activities) {
        var worker = testEnv.newWorker(TaskQueue.REMITTANCE_LIFECYCLE.getName());
        worker.registerWorkflowImplementationTypes(RemittanceLifecycleWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        return worker;
    }

    @Bean
    Worker walletFundingWorker(
            TestWorkflowEnvironment testEnv,
            WalletFundingActivities activities) {
        var worker = testEnv.newWorker(TaskQueue.WALLET_FUNDING.getName());
        worker.registerWorkflowImplementationTypes(WalletFundingWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        return worker;
    }

    @Bean
    TemporalEnvironmentStarter temporalEnvironmentStarter(
            TestWorkflowEnvironment testEnv, List<Worker> workers) {
        testEnv.start();
        return new TemporalEnvironmentStarter(workers.size());
    }

    public record TemporalEnvironmentStarter(int workerCount) {}
}
