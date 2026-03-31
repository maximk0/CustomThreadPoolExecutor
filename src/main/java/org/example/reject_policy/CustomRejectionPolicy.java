package org.example.reject_policy;

import org.example.CustomThreadPoolExecutor;

public interface CustomRejectionPolicy {
    void reject(Runnable task, CustomThreadPoolExecutor executor);
}