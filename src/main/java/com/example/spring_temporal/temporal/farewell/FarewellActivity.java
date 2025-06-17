package com.example.spring_temporal.temporal.farewell;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface FarewellActivity {
    @ActivityMethod
    String sayGoodbye(String name);
}
