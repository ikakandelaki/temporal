package com.example.spring_temporal.temporal.farewell;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface FarewellWorkflow {
    @WorkflowMethod
    String sayGoodbye(String name);
}
