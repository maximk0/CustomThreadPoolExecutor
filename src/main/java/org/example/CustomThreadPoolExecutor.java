package org.example;

import org.example.reject_policy.CustomCallerRunsPolicy;
import org.example.reject_policy.CustomRejectionPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPoolExecutor implements CustomExecutor {

    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueSize;
    private final int minSpareThreads;
    private final boolean loggingEnabled;

    private final CustomRejectionPolicy rejectionPolicy;
    private final CustomThreadFactory threadFactory;

    private final List<Worker> workers = new ArrayList<>();
    private final Object lock = new Object();
    private final AtomicInteger rrIndex = new AtomicInteger(0);

    private volatile boolean shutdown = false;

    public CustomThreadPoolExecutor(
            int corePoolSize,
            int maxPoolSize,
            long keepAliveTime,
            TimeUnit timeUnit,
            int queueSize,
            int minSpareThreads
    ) {
        this(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                timeUnit,
                queueSize,
                minSpareThreads,
                new CustomCallerRunsPolicy(),
                "MyPool",
                true
        );
    }

    public CustomThreadPoolExecutor(
            int corePoolSize,
            int maxPoolSize,
            long keepAliveTime,
            TimeUnit timeUnit,
            int queueSize,
            int minSpareThreads,
            CustomRejectionPolicy rejectionPolicy,
            String poolName
    ) {
        this(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                timeUnit,
                queueSize,
                minSpareThreads,
                rejectionPolicy,
                poolName,
                true
        );
    }

    public CustomThreadPoolExecutor(
            int corePoolSize,
            int maxPoolSize,
            long keepAliveTime,
            TimeUnit timeUnit,
            int queueSize,
            int minSpareThreads,
            CustomRejectionPolicy rejectionPolicy,
            String poolName,
            boolean loggingEnabled
    ) {
        if (corePoolSize < 0 || maxPoolSize <= 0 || maxPoolSize < corePoolSize) throw new IllegalArgumentException("Invalid pool sizes");
        if (keepAliveTime < 0) throw new IllegalArgumentException("keepAliveTime must be >= 0");
        if (queueSize <= 0) throw new IllegalArgumentException("queueSize must be > 0");
        if (minSpareThreads < 0) throw new IllegalArgumentException("minSpareThreads must be >= 0");

        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.queueSize = queueSize;
        this.minSpareThreads = minSpareThreads;
        this.rejectionPolicy = rejectionPolicy;
        this.loggingEnabled = loggingEnabled;
        this.threadFactory = new CustomThreadFactory(poolName, loggingEnabled);

        synchronized (lock) {
            for (int i = 0; i < corePoolSize; i++) {
                addWorker();
            }
        }
    }

    @Override
    public void execute(Runnable command) {
        if (command == null) throw new NullPointerException("command is null");

        if (shutdown) {
            log("[Rejected] Task " + command + " was rejected due to overload.");
            rejectionPolicy.reject(command, this);
            return;
        }

        Worker targetWorker;

        synchronized (lock) {
            ensureMinSpareThreads();

            targetWorker = selectWorkerRoundRobin();

            if (targetWorker != null) {
                boolean offered = targetWorker.offer(command);
                if (offered) {
                    logTaskAccepted(targetWorker, command);
                    return;
                }
            }

            if (workers.size() < maxPoolSize) {
                Worker newWorker = addWorker();
                boolean offered = newWorker.offer(command);
                if (offered) {
                    logTaskAccepted(newWorker, command);
                    return;
                }
            }
        }

        log("[Rejected] Task " + command + " was rejected due to overload.");
        rejectionPolicy.reject(command, this);
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        if (callable == null) throw new NullPointerException("callable is null");

        FutureTask<T> futureTask = new FutureTask<>(callable);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public void shutdown() {
        shutdown = true;
        log("[Pool] Shutdown initiated. New tasks will not be accepted.");
    }

    @Override
    public void shutdownNow() {
        shutdown = true;
        log("[Pool] ShutdownNow initiated. Interrupting all workers.");

        synchronized (lock) {
            for (Worker worker : workers) {
                worker.interrupt();
            }
        }
    }

    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);

        while (System.nanoTime() < deadline) {
            synchronized (lock) {
                if (workers.isEmpty()) return;
            }
            Thread.sleep(10);
        }
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public int getWorkerCount() {
        synchronized (lock) {
            return workers.size();
        }
    }

    public int getIdleWorkerCount() {
        synchronized (lock) {
            int count = 0;
            for (Worker worker : workers) {
                if (worker.isIdle()) count++;
            }
            return count;
        }
    }

    private void ensureMinSpareThreads() {
        int idleCount = 0;
        for (Worker worker : workers) {
            if (worker.isIdle()) idleCount++;
        }

        while (idleCount < minSpareThreads && workers.size() < maxPoolSize) {
            addWorker();
            idleCount++;
        }
    }

    private Worker selectWorkerRoundRobin() {
        if (workers.isEmpty()) return null;

        int index = rrIndex.getAndIncrement() % workers.size();
        return workers.get(index);
    }

    private Worker addWorker() {
        Worker worker = new Worker(queueSize);
        workers.add(worker);
        worker.start();
        return worker;
    }

    private void removeWorker(Worker worker) {
        synchronized (lock) {
            workers.remove(worker);
        }
    }

    private void logTaskAccepted(Worker worker, Runnable task) {
        log("[Pool] Task accepted into queue #" + worker.getId() + ": " + task);
    }

    private void log(String message) {
        if (loggingEnabled) {
            System.out.println(message);
        }
    }

    private final class Worker implements Runnable {
        private static final AtomicInteger WORKER_ID_GEN = new AtomicInteger(0);

        private final int id;
        private final BlockingQueue<Runnable> queue;
        private final Thread thread;

        private volatile boolean busy = false;

        Worker(int queueCapacity) {
            this.id = WORKER_ID_GEN.incrementAndGet();
            this.queue = new ArrayBlockingQueue<>(queueCapacity);
            this.thread = threadFactory.newThread(this);
        }

        int getId() {
            return id;
        }

        boolean offer(Runnable task) {
            return queue.offer(task);
        }

        boolean isIdle() {
            return !busy && queue.isEmpty();
        }

        void start() {
            thread.start();
        }

        void interrupt() {
            thread.interrupt();
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if (shutdown && queue.isEmpty()) break;

                    Runnable task;
                    try {
                        task = queue.poll(keepAliveTime, timeUnit);
                    } catch (InterruptedException e) {
                        if (shutdown) break;
                        continue;
                    }

                    if (task == null) {
                        synchronized (lock) {
                            if (workers.size() > corePoolSize) {
                                log("[Worker] " + Thread.currentThread().getName() + " idle timeout, stopping.");
                                removeWorker(this);
                                return;
                            }
                        }
                        continue;
                    }

                    if (shutdown) {
                        log("[Worker] " + Thread.currentThread().getName() + " skips task because pool is shutting down: " + task);
                        continue;
                    }

                    busy = true;
                    try {
                        log("[Worker] " + Thread.currentThread().getName() + " executes " + task);
                        task.run();
                    } catch (Throwable t) {
                        log("[Worker] " + Thread.currentThread().getName() + " task failed: " + t.getMessage());
                        if (loggingEnabled) t.printStackTrace();
                    } finally {
                        busy = false;
                    }
                }
            } finally {
                removeWorker(this);
                log("[Worker] " + Thread.currentThread().getName() + " terminated.");
            }
        }
    }
}