package com.example.spring_temporal.temporal.impl;

import com.example.spring_temporal.domain.Company;
import com.example.spring_temporal.temporal.CompanyMigrationWorkflow;
import com.example.spring_temporal.temporal.MigrationStarterWorkflow;
import com.example.spring_temporal.temporal.MigrationWorkflowStatus;
import io.temporal.client.WorkflowStub;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
@WorkflowImpl(taskQueues = "${app.temporal.migration-queue}")
public class MigrationStarterWorkflowImpl implements MigrationStarterWorkflow {
    private final Set<String> finishedMigrationWorkflowIds = new HashSet<>();
    private final Set<String> readyToCommitMigrationWorkflowIds = new HashSet<>();
    private final Set<String> failedWorkflowIds = new HashSet<>();

    @Override
    public void start(Company rootCompany, Set<Company> childrenCompanies) {
        System.out.println("Starting Migration Starter Workflow");

        String starterWorkflowId = Workflow.getInfo().getWorkflowId();
        Set<String> startedCompanyMigrationWorkflowIds = new HashSet<>();
        List<CompanyMigrationWorkflow> startedCompanyMigrationWorkflows = new ArrayList<>();

        String rootCompanyMigrationWorkflowId = "%s-root-company-%s".formatted(starterWorkflowId, rootCompany.id());
        CompanyMigrationWorkflow rootCompanyMigrationWorkflow = Workflow.newChildWorkflowStub(CompanyMigrationWorkflow.class,
                childWorkflowOptions(rootCompanyMigrationWorkflowId));
        Async.procedure(rootCompanyMigrationWorkflow::migrate, rootCompany);

        startedCompanyMigrationWorkflowIds.add(rootCompanyMigrationWorkflowId);
        startedCompanyMigrationWorkflows.add(rootCompanyMigrationWorkflow);

        if (childrenCompanies != null && !childrenCompanies.isEmpty()) {
            for (Company childCompany : childrenCompanies) {
                String childWorkflowId = "%s-child-company-%s".formatted(starterWorkflowId, childCompany.id());
                CompanyMigrationWorkflow childCompanyMigrationWorkflow = Workflow.newChildWorkflowStub(CompanyMigrationWorkflow.class,
                        childWorkflowOptions(childWorkflowId));
                Async.procedure(childCompanyMigrationWorkflow::migrate, childCompany);

                startedCompanyMigrationWorkflowIds.add(childWorkflowId);
                startedCompanyMigrationWorkflows.add(childCompanyMigrationWorkflow);
            }
        }

        Workflow.await(() -> readyToCommitMigrationWorkflowIds.equals(startedCompanyMigrationWorkflowIds) || !failedWorkflowIds.isEmpty());
        commitOrRollbackAllCompanies(startedCompanyMigrationWorkflowIds, startedCompanyMigrationWorkflows);
        Workflow.await(() -> finishedMigrationWorkflowIds.equals(startedCompanyMigrationWorkflowIds));
        resetSignalVariables();
        System.out.println("Ended Migration Starter Workflow");
    }

    private ChildWorkflowOptions childWorkflowOptions(String workflowId) {
        return ChildWorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(Workflow.getInfo().getTaskQueue())
                .build();
    }

    private void commitOrRollbackAllCompanies(
            Set<String> startedCompanyMigrationWorkflowIds,
            List<CompanyMigrationWorkflow> startedCompanyMigrationWorkflows
    ) {
        boolean commit = readyToCommitMigrationWorkflowIds.equals(startedCompanyMigrationWorkflowIds);
        if (commit) {
            startedCompanyMigrationWorkflows.forEach(CompanyMigrationWorkflow::signalCommit);
        }
        if (!failedWorkflowIds.isEmpty()) {
            startedCompanyMigrationWorkflows.stream()
                    .map(WorkflowStub::fromTyped)
                    .filter(workflowStub -> !failedWorkflowIds.contains(workflowStub.getExecution().getWorkflowId()))
                    .forEach(WorkflowStub::cancel);
        }
    }

    private void resetSignalVariables() {
        readyToCommitMigrationWorkflowIds.clear();
        finishedMigrationWorkflowIds.clear();
        failedWorkflowIds.clear();
    }

    @Override
    public void signalMigrationWorkflowState(MigrationWorkflowState migrationWorkflowState) {
        if (migrationWorkflowState.workflowStatus() == MigrationWorkflowStatus.READY_TO_COMMIT) {
            readyToCommitMigrationWorkflowIds.add(migrationWorkflowState.migrationWorkflowId());
        }
        if (migrationWorkflowState.workflowStatus() == MigrationWorkflowStatus.FINISHED) {
            finishedMigrationWorkflowIds.add(migrationWorkflowState.migrationWorkflowId());
        }
        if (migrationWorkflowState.workflowStatus() == MigrationWorkflowStatus.READY_TO_ROLLBACK) {
            failedWorkflowIds.add(migrationWorkflowState.migrationWorkflowId());
        }
    }
}
