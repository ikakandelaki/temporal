package com.example.spring_temporal.config;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.TemporalOptionsCustomizer;
import io.temporal.worker.WorkflowImplementationOptions;
import jakarta.annotation.Nonnull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class TemporalOptionsConfig {

    private final ActivityOptions activityOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(5))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(5)
                    .build())
            .build();


    @Bean
    public TemporalOptionsCustomizer<WorkflowImplementationOptions.Builder>
    customWorkflowImplementationOptions() {
        return new TemporalOptionsCustomizer<>() {
            @Nonnull
            @Override
            public WorkflowImplementationOptions.Builder customize(
                    @Nonnull WorkflowImplementationOptions.Builder optionsBuilder) {
                optionsBuilder.setDefaultActivityOptions(activityOptions);
                return optionsBuilder;
            }
        };
    }
}
