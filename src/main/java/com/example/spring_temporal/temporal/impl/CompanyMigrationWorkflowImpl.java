package com.example.spring_temporal.temporal.impl;

import com.example.spring_temporal.domain.Company;
import com.example.spring_temporal.temporal.CompanyMigrationActivity;
import com.example.spring_temporal.temporal.CompanyMigrationWorkflow;
import com.example.spring_temporal.temporal.MigrationStarterWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.CanceledFailure;
import io.temporal.failure.TemporalFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;

import java.time.Duration;

@SuppressWarnings("unused")
@WorkflowImpl(taskQueues = "${app.temporal.migration-queue}")
public class CompanyMigrationWorkflowImpl implements CompanyMigrationWorkflow {
    Saga saga = new Saga(new Saga.Options.Builder().build());
    private boolean commit = false;

    private ActivityOptions getActivityOptions() {
        return ActivityOptions.newBuilder()
                .setTaskQueue(Workflow.getInfo().getTaskQueue())
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                .setStartToCloseTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Override
    public void migrate(Company company) {
        String workflowId = Workflow.getInfo().getWorkflowId();

        CompanyMigrationActivity companyMigrationActivity = Workflow.newActivityStub(CompanyMigrationActivity.class, getActivityOptions());

        try {
            saga.addCompensation(companyMigrationActivity::compensateActivity, "step-1", workflowId);
            if ("FAIL_COMPANY".equals(company.name())) {
                companyMigrationActivity.executeActivity("step-1", workflowId, Duration.ofSeconds(3));
                companyMigrationActivity.failingActivity(workflowId);
            } else {
                companyMigrationActivity.executeActivity("step-1", workflowId, Duration.ofSeconds(9));
                saga.addCompensation(companyMigrationActivity::compensateActivity, "step-2", workflowId);
                companyMigrationActivity.executeActivity("step-2", workflowId, Duration.ofMillis(1));
            }

            sendReadyToCommitSignalToParent();
            Workflow.await(() -> commit);
            companyMigrationActivity.executeActivity("commit", workflowId, Duration.ofMillis(1));
            commit = false;
        } catch (TemporalFailure e) {
            if (e.getCause() instanceof CanceledFailure) {
                Workflow.newDetachedCancellationScope(() -> saga.compensate()).run();
            } else {
                saga.compensate();
                sendReadyToRollbackSignalToParent();
            }
            throw e;
        }
    }

    private void sendReadyToCommitSignalToParent() {
        Workflow.getInfo().getParentWorkflowId().ifPresent(workflowId -> {
            MigrationStarterWorkflow parentWorkflow = Workflow.newExternalWorkflowStub(MigrationStarterWorkflow.class, workflowId);
            parentWorkflow.signalMigrationWorkflowState(MigrationStarterWorkflow.MigrationWorkflowState.ofReadyToCommit(Workflow.getInfo().getWorkflowId()));
        });
    }

    private void sendReadyToRollbackSignalToParent() {
        Workflow.getInfo().getParentWorkflowId().ifPresent(workflowId -> {
            MigrationStarterWorkflow parentWorkflow = Workflow.newExternalWorkflowStub(MigrationStarterWorkflow.class, workflowId);
            parentWorkflow.signalMigrationWorkflowState(MigrationStarterWorkflow.MigrationWorkflowState.ofReadyToRollback(Workflow.getInfo().getWorkflowId()));
        });
    }

    @Override
    public void signalCommit() {
        commit = true;
    }
}
