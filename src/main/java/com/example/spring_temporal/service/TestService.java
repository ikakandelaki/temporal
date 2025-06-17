package com.example.spring_temporal.service;

import com.example.spring_temporal.temporal.farewell.FarewellWorkflow;
import com.example.spring_temporal.temporal.greeting.HelloWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TestService {
    private final WorkflowClient greetWorkflowClient;
    private final WorkflowClient goodbyeWorkflowClient;

    public TestService(
            @Qualifier("greetWorkflowClient") WorkflowClient greetWorkflowClient,
            @Qualifier("goodbyeWorkflowClient") WorkflowClient goodbyeWorkflowClient
    ) {
        this.greetWorkflowClient = greetWorkflowClient;
        this.goodbyeWorkflowClient = goodbyeWorkflowClient;
    }

    @Value("${app.temporal.greeting-task-queue}")
    private String greetingTaskQueue;

    @Value("${app.temporal.farewell-task-queue}")
    private String farewellTaskQueue;

    public String test() {
        try {
            HelloWorkflow helloWorkflow = greetWorkflowClient.newWorkflowStub(
                    HelloWorkflow.class,
                    getWorkflowOptions("hello-workflow", greetingTaskQueue)
            );
            String saidHello = helloWorkflow.sayHello("Irakli");

            FarewellWorkflow farewellWorkflow = goodbyeWorkflowClient.newWorkflowStub(
                    FarewellWorkflow.class,
                    getWorkflowOptions("farewell-workflow", farewellTaskQueue)
            );
            String saidGoodbye = farewellWorkflow.sayGoodbye("Irakli");
            return saidHello + " and " + saidGoodbye;
        } catch (Exception e) {
            return "Exception: " + e.getMessage();
        }
    }

    private WorkflowOptions getWorkflowOptions(String workflowId, String taskQueue) {
        return WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .build();
    }
}
