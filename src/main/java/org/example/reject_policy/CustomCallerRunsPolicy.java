package org.example.reject_policy;

import org.example.CustomThreadPoolExecutor;

public class CustomCallerRunsPolicy implements CustomRejectionPolicy {
    @Override
    public void reject(
            Runnable task,
            CustomThreadPoolExecutor executor
    ) {
        if (!executor.isShutdown()) task.run();
    }
}