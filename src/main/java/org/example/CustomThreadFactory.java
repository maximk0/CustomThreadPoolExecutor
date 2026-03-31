package org.example;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadFactory implements ThreadFactory {
    private final AtomicInteger counter = new AtomicInteger(0);
    private final String poolName;
    private final boolean loggingEnabled;

    public CustomThreadFactory(String poolName, boolean loggingEnabled) {
        this.poolName = poolName;
        this.loggingEnabled = loggingEnabled;
    }

    @Override
    public Thread newThread(Runnable r) {
        String threadName = poolName + "-worker-" + counter.incrementAndGet();
        log("[ThreadFactory] Creating new thread: " + threadName);

        return new Thread(() -> {
            try {
                r.run();
            } finally {
                log("[ThreadFactory] Thread finished: " + threadName);
            }
        }, threadName);
    }

    private void log(String message) {
        if (loggingEnabled) {
            System.out.println(message);
        }
    }
}