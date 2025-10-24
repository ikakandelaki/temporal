package com.example.spring_temporal.temporal.impl;

import com.example.spring_temporal.domain.Company;
import com.example.spring_temporal.temporal.CompanyMigrationActivity;
import com.example.spring_temporal.temporal.CompanyMigrationOrchestratorWorkflow;
import com.example.spring_temporal.temporal.CompanyMigrationWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.CanceledFailure;
import io.temporal.failure.TemporalFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@WorkflowImpl(taskQueues = "${app.temporal.migration-queue}")
public class CompanyMigrationWorkflowImpl implements CompanyMigrationWorkflow {
    Saga saga = new Saga(new Saga.Options.Builder().build());

    private CancellationScope cancellationScope;

    private final AtomicBoolean commitSignalReceived = new AtomicBoolean(false);
    private final AtomicBoolean isFailed = new AtomicBoolean(false);
    private final AtomicBoolean rollbackSignalReceived = new AtomicBoolean(false);

    private ActivityOptions getActivityOptions() {
        return ActivityOptions.newBuilder()
                .setTaskQueue(Workflow.getInfo().getTaskQueue())
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                .setStartToCloseTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Override
    public void migrate(Company company) {
        cancellationScope = Workflow.newCancellationScope(() -> {
            String workflowId = Workflow.getInfo().getWorkflowId();
            try {
                CompanyMigrationActivity companyMigrationActivity = Workflow.newActivityStub(CompanyMigrationActivity.class,
                        getActivityOptions());

                executeSteps(company, companyMigrationActivity, workflowId);
                sendMigrationWorkflowCommitReadinessSignalToOrchestrator(workflowId);
                Workflow.await(commitSignalReceived::get);
                companyMigrationActivity.executeActivity("commit", workflowId, Duration.ofMillis(1));
            } catch (TemporalFailure e) {
                if (e.getCause() instanceof CanceledFailure canceled) {
                    saga.compensate();
                    throw canceled;
                } else {
                    isFailed.set(true);
                    sendMigrationWorkflowFailureSignalToOrchestrator();
                    Workflow.await(rollbackSignalReceived::get);
                    saga.compensate();
                    throw e;
                }
            } finally {
                commitSignalReceived.set(false);
                isFailed.set(false);
                rollbackSignalReceived.set(false);
            }
        });
        cancellationScope.run();
    }

    private void executeSteps(Company company, CompanyMigrationActivity companyMigrationActivity, String workflowId) {
        if ("FAIL_COMPANY".equals(company.name())) {
            saga.addCompensation(companyMigrationActivity::compensateActivity, "step-1", workflowId);
            companyMigrationActivity.executeActivity("step-1", workflowId, Duration.ofSeconds(3));
            companyMigrationActivity.failingActivity(workflowId);
        } else {
            saga.addCompensation(companyMigrationActivity::compensateActivity, "step-1", workflowId);
            companyMigrationActivity.executeActivity("step-1", workflowId, Duration.ofSeconds(2));
            saga.addCompensation(companyMigrationActivity::compensateActivity, "step-2", workflowId);
            companyMigrationActivity.executeActivity("step-2", workflowId, Duration.ofSeconds(3));
        }
    }

    private void sendMigrationWorkflowCommitReadinessSignalToOrchestrator(String workflowId) {
        Workflow.getInfo().getParentWorkflowId().ifPresent(orchestratorWorkflowId -> {
            CompanyMigrationOrchestratorWorkflow parentWorkflow = Workflow.newExternalWorkflowStub(
                    CompanyMigrationOrchestratorWorkflow.class, orchestratorWorkflowId);
            parentWorkflow.signalMigrationWorkflowReadinessForCommit(workflowId);
        });
    }

    private void sendMigrationWorkflowFailureSignalToOrchestrator() {
        Workflow.getInfo().getParentWorkflowId().ifPresent(orchestratorWorkflowId -> {
            CompanyMigrationOrchestratorWorkflow orchestratorWorkflow = Workflow.newExternalWorkflowStub(
                    CompanyMigrationOrchestratorWorkflow.class, orchestratorWorkflowId);
            orchestratorWorkflow.signalMigrationWorkflowFailure();
        });
    }

    @Override
    public void signalCommit() {
        commitSignalReceived.set(true);
    }

    @Override
    public void signalRollback() {
        if (isFailed.get()) {
            rollbackSignalReceived.set(true);
            return;
        }
        cancellationScope.cancel();
    }
}
