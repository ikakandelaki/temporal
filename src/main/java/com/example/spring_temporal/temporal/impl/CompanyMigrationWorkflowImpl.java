package com.example.spring_temporal.temporal.impl;

import com.example.spring_temporal.domain.Company;
import com.example.spring_temporal.temporal.CompanyMigrationActivity;
import com.example.spring_temporal.temporal.CompanyMigrationWorkflow;
import com.example.spring_temporal.temporal.MigrationStarterWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
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
    private boolean rollback = false;

    @Override
    public void migrate(Company company) {
        CompanyMigrationActivity companyMigrationActivity = Workflow.newActivityStub(CompanyMigrationActivity.class,
                ActivityOptions.newBuilder()
                        .setTaskQueue(Workflow.getInfo().getTaskQueue())
                        .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                        .setStartToCloseTimeout(Duration.ofSeconds(10))
                        .build()
        );

        try {
            saga.addCompensation(companyMigrationActivity::compensateActivity, "step-1");
            companyMigrationActivity.executeActivity("step-1", Workflow.getInfo().getWorkflowId());

            // fail if company name = FAIL_COMPANY
            if ("FAIL_COMPANY".equals(company.name())) {
                companyMigrationActivity.failingActivity();
            }

            sendReadyToCommitSignalToParent();
            commitOrRollback(companyMigrationActivity);
        } catch (TemporalFailure e) {
            sendReadyToRollbackSignalToParent();
            Workflow.await(() -> rollback);
            saga.compensate();
            throw e;
        } finally {
            sendFinishSignalToParent();
            rollback = false;
            commit = false;
        }
    }

    private void sendReadyToCommitSignalToParent() {
        Workflow.getInfo().getParentWorkflowId().ifPresent(workflowId -> {
            MigrationStarterWorkflow parentWorkflow = Workflow.newExternalWorkflowStub(MigrationStarterWorkflow.class, workflowId);
            parentWorkflow.signalMigrationWorkflowState(MigrationStarterWorkflow.MigrationWorkflowState.ofReadyToCommit(Workflow.getInfo().getWorkflowId()));
        });
    }

    private void commitOrRollback(CompanyMigrationActivity companyMigrationActivity) {
        Workflow.await(() -> commit || rollback);
        if (commit) {
            companyMigrationActivity.executeActivity("commit", Workflow.getInfo().getWorkflowId());
        }
        if (rollback) {
            saga.compensate();
        }
    }

    private void sendReadyToRollbackSignalToParent() {
        Workflow.getInfo().getParentWorkflowId().ifPresent(workflowId -> {
            MigrationStarterWorkflow parentWorkflow = Workflow.newExternalWorkflowStub(MigrationStarterWorkflow.class, workflowId);
            parentWorkflow.signalMigrationWorkflowState(MigrationStarterWorkflow.MigrationWorkflowState.ofReadyToRollback(Workflow.getInfo().getWorkflowId()));
        });
    }

    private void sendFinishSignalToParent() {
        Workflow.getInfo().getParentWorkflowId().ifPresent(workflowId -> {
            MigrationStarterWorkflow parentWorkflow = Workflow.newExternalWorkflowStub(MigrationStarterWorkflow.class, workflowId);
            parentWorkflow.signalMigrationWorkflowState(MigrationStarterWorkflow.MigrationWorkflowState.ofFinished(Workflow.getInfo().getWorkflowId()));
        });
    }

    @Override
    public void signalRollback() {
        rollback = true;
    }

    @Override
    public void signalCommit() {
        commit = true;
    }
}
