package com.example.spring_temporal.temporal.impl;

import com.example.spring_temporal.temporal.CompanyMigrationActivity;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(taskQueues = "${app.temporal.migration-queue}")
public class CompanyMigrationActivityImpl implements CompanyMigrationActivity {
    @Override
    public void executeActivity(String activityName, String workflowId) {
        System.out.printf("Executing activity '%s' in workflow: %s%n", activityName, workflowId);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            System.out.println("Failed to execute activity: " + activityName);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void compensateActivity(String activityName) {
        System.out.println("Compensating activity: " + activityName);
    }

    @Override
    public void failingActivity() {
        throw new RuntimeException("Failing activity");
    }
}
