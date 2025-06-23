package com.example.spring_temporal.temporal.worker;

import com.example.spring_temporal.temporal.GreetingActivity;
import com.example.spring_temporal.temporal.GreetingWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;

@WorkflowImpl(taskQueues = "${app.temporal.task-queue}")
public class GreetingWorkflowImpl implements GreetingWorkflow {
    @Override
    public String sayHi(String name) {
//        ActivityOptions opts = ActivityOptions.newBuilder()
//                .setTaskQueue("greeting-queue")
//                .build();

        GreetingActivity greetingActivity = Workflow.newActivityStub(GreetingActivity.class);
        return greetingActivity.sayHi(name);
    }
}
