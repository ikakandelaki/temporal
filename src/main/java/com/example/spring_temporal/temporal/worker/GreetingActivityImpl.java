package com.example.spring_temporal.temporal.worker;

import com.example.spring_temporal.temporal.GreetingActivity;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@ActivityImpl(taskQueues = "${app.temporal.task-queue}")
@Component
public class GreetingActivityImpl implements GreetingActivity {
    @Override
    public String sayHi(String name) {
        return "Hello " + name;
    }
}
