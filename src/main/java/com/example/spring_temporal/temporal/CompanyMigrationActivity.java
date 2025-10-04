package com.example.spring_temporal.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface CompanyMigrationActivity {
    @ActivityMethod
    void executeActivity(String activityName, String workflowId);

    @ActivityMethod
    void compensateActivity(String activityName);

    @ActivityMethod
    void failingActivity();
}
