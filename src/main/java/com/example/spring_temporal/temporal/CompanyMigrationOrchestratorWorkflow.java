package com.example.spring_temporal.temporal;

import com.example.spring_temporal.domain.Company;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.Set;

@WorkflowInterface
public interface CompanyMigrationOrchestratorWorkflow {
    @WorkflowMethod
    void start(Company rootCompany, Set<Company> childrenCompanies);

    @SignalMethod
    void signalMigrationWorkflowReadinessForCommit(String migrationWorkflowId);

    @SignalMethod
    void signalMigrationWorkflowFailure(String migrationWorkflowId);
}
