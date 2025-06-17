package com.example.spring_temporal.temporal.farewell;

import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@ActivityImpl(taskQueues = "${app.temporal.farewell-task-queue}")
@Component
public class FarewellActivityImpl implements FarewellActivity {
    @Override
    public String sayGoodbye(String name) {
        return "Goodbye " + name;
    }
}
