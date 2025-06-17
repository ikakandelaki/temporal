package com.example.spring_temporal.temporal.farewell;

import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;

import java.time.Duration;

@WorkflowImpl(taskQueues = "${app.temporal.farewell-task-queue}")
public class FarewellWorkflowImpl implements FarewellWorkflow {
    @Override
    public String sayGoodbye(String name) {
        ActivityOptions options = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(5))
                .build();

        FarewellActivity farewellActivity = Workflow.newActivityStub(FarewellActivity.class, options);
        return farewellActivity.sayGoodbye(name);
    }
}
