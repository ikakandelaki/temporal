package com.example.spring_temporal.temporal;

import com.example.spring_temporal.domain.Company;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CompanyMigrationWorkflow {
    @WorkflowMethod
    void migrate(Company company);

    @SignalMethod
    void signalRollback();

    @SignalMethod
    void signalCommit();
}
