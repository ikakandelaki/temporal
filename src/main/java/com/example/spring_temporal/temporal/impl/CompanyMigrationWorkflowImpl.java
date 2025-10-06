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
            Workflow.await(() -> commit);
            companyMigrationActivity.executeActivity("commit", Workflow.getInfo().getWorkflowId());
        } catch (CanceledFailure e) {
            System.out.println("Cancelling: " + Workflow.getInfo().getWorkflowId());
            saga.compensate();
            throw e;
        } catch (TemporalFailure e) {
            sendReadyToRollbackSignalToParent();
            saga.compensate();
            throw e;
        } finally {
            sendFinishSignalToParent();
            commit = false;
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

    private void sendFinishSignalToParent() {
        Workflow.getInfo().getParentWorkflowId().ifPresent(workflowId -> {
            MigrationStarterWorkflow parentWorkflow = Workflow.newExternalWorkflowStub(MigrationStarterWorkflow.class, workflowId);
            parentWorkflow.signalMigrationWorkflowState(MigrationStarterWorkflow.MigrationWorkflowState.ofFinished(Workflow.getInfo().getWorkflowId()));
        });
    }

    @Override
    public void signalCommit() {
        commit = true;
    }
}
