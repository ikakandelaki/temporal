package com.example.spring_temporal.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfig {

    @Bean
    public WorkflowClient greetWorkflowClient(WorkflowServiceStubs stubs) {
        return WorkflowClient.newInstance(stubs, WorkflowClientOptions.newBuilder()
                .setNamespace("greeting")
                .build());
    }

    @Bean
    public WorkflowClient goodbyeWorkflowClient(WorkflowServiceStubs stubs) {
        return WorkflowClient.newInstance(stubs, WorkflowClientOptions.newBuilder()
                .setNamespace("farewell")
                .build());
    }
}
