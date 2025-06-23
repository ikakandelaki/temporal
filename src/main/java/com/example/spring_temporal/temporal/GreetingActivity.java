package com.example.spring_temporal.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface GreetingActivity {
    @ActivityMethod
    String sayHi(String name);
}
