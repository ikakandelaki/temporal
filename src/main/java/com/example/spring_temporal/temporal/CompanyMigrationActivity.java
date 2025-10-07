package com.example.spring_temporal.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.time.Duration;

@ActivityInterface
public interface CompanyMigrationActivity {
    @ActivityMethod
    void executeActivity(String activityName, String workflowId, Duration duration);

    @ActivityMethod
    void compensateActivity(String activityName, String workflowId);

    @ActivityMethod
    void failingActivity(String workflowId);
}
