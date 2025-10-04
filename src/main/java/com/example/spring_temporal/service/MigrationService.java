package com.example.spring_temporal.service;

import com.example.spring_temporal.domain.Company;
import com.example.spring_temporal.temporal.MigrationStarterWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MigrationService {
    private final WorkflowClient workflowClient;

    @Value("${app.temporal.migration-queue}")
    private String taskQueue;

    public void startMigration(Company rootCompany, Set<Company> childrenCompanies) {
        MigrationStarterWorkflow migrationStarterWorkflow = workflowClient.newWorkflowStub(MigrationStarterWorkflow.class,
                getWorkflowOptions("group-" + UUID.randomUUID()));
        WorkflowClient.start(migrationStarterWorkflow::start, rootCompany, childrenCompanies);
    }

    private WorkflowOptions getWorkflowOptions(String workflowId) {
        return WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .build();
    }
}
