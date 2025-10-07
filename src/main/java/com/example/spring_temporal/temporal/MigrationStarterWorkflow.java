package com.example.spring_temporal.temporal;

import com.example.spring_temporal.domain.Company;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.Set;

@WorkflowInterface
public interface MigrationStarterWorkflow {
    @WorkflowMethod
    void start(Company rootCompany, Set<Company> childrenCompanies);

    @SignalMethod
    void signalMigrationWorkflowState(MigrationWorkflowState migrationWorkflowState);

    record MigrationWorkflowState(String migrationWorkflowId, MigrationWorkflowStatus workflowStatus) {
        public static MigrationWorkflowState ofReadyToRollback(String migrationWorkflowId) {
            return new MigrationWorkflowState(migrationWorkflowId, MigrationWorkflowStatus.READY_TO_ROLLBACK);
        }

        public static MigrationWorkflowState ofReadyToCommit(String migrationWorkflowId) {
            return new MigrationWorkflowState(migrationWorkflowId, MigrationWorkflowStatus.READY_TO_COMMIT);
        }
    }
}
