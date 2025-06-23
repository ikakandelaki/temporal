package com.example.spring_temporal.service;

import com.example.spring_temporal.temporal.GreetingWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TestService {
    private final WorkflowClient workflowClient;

    @Value("${app.temporal.task-queue}")
    private String queue;

    public TestService(
            WorkflowClient workflowClient
    ) {
        this.workflowClient = workflowClient;
    }

    public String test() {
        try {
            WorkflowOptions wfOptions = WorkflowOptions.newBuilder()
                    .setWorkflowId("greeting-wf")
                    .setTaskQueue(queue)
                    .build();

            GreetingWorkflow greetingWorkflow = workflowClient.newWorkflowStub(
                    GreetingWorkflow.class,
                    wfOptions
            );
            return greetingWorkflow.sayHi("Irakli");
        } catch (Exception e) {
            return "Exception: " + e.getMessage();
        }
    }
}
