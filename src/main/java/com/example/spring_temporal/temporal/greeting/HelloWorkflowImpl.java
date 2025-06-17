package com.example.spring_temporal.temporal.greeting;

import io.temporal.spring.boot.WorkflowImpl;

@WorkflowImpl(taskQueues = "${app.temporal.greeting-task-queue}")
public class HelloWorkflowImpl implements HelloWorkflow {
    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }
}
