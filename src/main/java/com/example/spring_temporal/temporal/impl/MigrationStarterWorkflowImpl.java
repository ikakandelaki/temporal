package com.example.spring_temporal.temporal.impl;

import com.example.spring_temporal.domain.Company;
import com.example.spring_temporal.temporal.CompanyMigrationWorkflow;
import com.example.spring_temporal.temporal.MigrationStarterWorkflow;
import com.example.spring_temporal.temporal.MigrationWorkflowStatus;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.failure.TemporalFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Async;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
@WorkflowImpl(taskQueues = "${app.temporal.migration-queue}")
public class MigrationStarterWorkflowImpl implements MigrationStarterWorkflow {
    private final Set<String> readyToCommitMigrationWorkflowIds = new HashSet<>();
    private String failedWorkflowId = null;

    private record CompanyMigrationWorkflowInfo(
            String workflowId,
            CompanyMigrationWorkflow migrationWorkflow,
            Promise<Void> migrationPromise,
            CancellationScope cancellationScope
    ) {

    }

    @Override
    public void start(Company rootCompany, Set<Company> childrenCompanies) {
        String starterWorkflowId = Workflow.getInfo().getWorkflowId();
        Set<String> startedCompanyMigrationWorkflowIds = new HashSet<>();
        List<CompanyMigrationWorkflowInfo> startedCompanyMigrationWorkflows = new ArrayList<>();

        String rootCompanyMigrationWorkflowId = "%s-root-company-%s".formatted(starterWorkflowId, rootCompany.id());
        startedCompanyMigrationWorkflows.add(startMigrationWorkflow(rootCompany, rootCompanyMigrationWorkflowId));
        startedCompanyMigrationWorkflowIds.add(rootCompanyMigrationWorkflowId);

        if (childrenCompanies != null && !childrenCompanies.isEmpty()) {
            for (Company childCompany : childrenCompanies) {
                String childWorkflowId = "%s-child-company-%s".formatted(starterWorkflowId, childCompany.id());
                startedCompanyMigrationWorkflows.add(startMigrationWorkflow(childCompany, childWorkflowId));
                startedCompanyMigrationWorkflowIds.add(childWorkflowId);
            }
        }

        Workflow.await(() -> allMigrationsAreReadyToCommit(startedCompanyMigrationWorkflowIds) || oneOfTheMigrationsFailed());
        commitOrRollbackAllCompanies(startedCompanyMigrationWorkflowIds, startedCompanyMigrationWorkflows);

        getMigrationWorkflowResults(startedCompanyMigrationWorkflows);

        boolean shouldFail = oneOfTheMigrationsFailed();
        resetSignalVariables();

        if (shouldFail) {
            throw ApplicationFailure.newNonRetryableFailure("", "");
        }
    }

    private CompanyMigrationWorkflowInfo startMigrationWorkflow(Company company, String workflowId) {
        ChildWorkflowOptions childWorkflowOptions = ChildWorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(Workflow.getInfo().getTaskQueue())
                .build();

        CompanyMigrationWorkflow migrationWorkflow = Workflow.newChildWorkflowStub(CompanyMigrationWorkflow.class,
                childWorkflowOptions);

        AtomicReference<Promise<Void>> promiseAtomicReference = new AtomicReference<>();
        CancellationScope cancellationScope = Workflow.newCancellationScope(() ->
                promiseAtomicReference.set(Async.procedure(migrationWorkflow::migrate, company)));
        cancellationScope.run();

        return new CompanyMigrationWorkflowInfo(workflowId, migrationWorkflow, promiseAtomicReference.get(), cancellationScope);
    }

    private boolean allMigrationsAreReadyToCommit(Set<String> startedCompanyMigrationWorkflowIds) {
        return readyToCommitMigrationWorkflowIds.equals(startedCompanyMigrationWorkflowIds);
    }

    private boolean oneOfTheMigrationsFailed() {
        return failedWorkflowId != null;
    }

    private void commitOrRollbackAllCompanies(
            Set<String> startedCompanyMigrationWorkflowIds,
            List<CompanyMigrationWorkflowInfo> startedCompanyMigrationWorkflows) {
        if (allMigrationsAreReadyToCommit(startedCompanyMigrationWorkflowIds)) {
            startedCompanyMigrationWorkflows.forEach(e -> e.migrationWorkflow().signalCommit());
        }
        if (oneOfTheMigrationsFailed()) {
            startedCompanyMigrationWorkflows.stream()
                    .filter(workflowInfo -> !failedWorkflowId.equals(workflowInfo.workflowId()))
                    .forEach(workflowInfo -> workflowInfo.cancellationScope().cancel());
        }
    }

    private void getMigrationWorkflowResults(List<CompanyMigrationWorkflowInfo> startedCompanyMigrationWorkflows) {
        startedCompanyMigrationWorkflows.forEach(workflowInfo -> {
            try {
                workflowInfo.migrationPromise().get();
                System.out.println("Migration completed: " + workflowInfo.workflowId());
            } catch (TemporalFailure e) {
                if (e.getCause() instanceof CanceledFailure) {
                    System.out.println("Workflow migration canceled: " + workflowInfo.workflowId());
                } else {
                    System.out.println("Workflow migration failed: " + workflowInfo.workflowId());
                }
            }
        });
    }

    private void resetSignalVariables() {
        readyToCommitMigrationWorkflowIds.clear();
        failedWorkflowId = null;
    }

    @Override
    public void signalMigrationWorkflowState(MigrationWorkflowState migrationWorkflowState) {
        if (migrationWorkflowState.workflowStatus() == MigrationWorkflowStatus.READY_TO_COMMIT) {
            readyToCommitMigrationWorkflowIds.add(migrationWorkflowState.migrationWorkflowId());
        }
        if (migrationWorkflowState.workflowStatus() == MigrationWorkflowStatus.READY_TO_ROLLBACK) {
            failedWorkflowId = migrationWorkflowState.migrationWorkflowId();
        }
    }
}
