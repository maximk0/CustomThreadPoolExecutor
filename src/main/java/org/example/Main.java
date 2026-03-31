package org.example;

import org.example.demo.DemoTask;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor(
                2,
                4,
                5,
                TimeUnit.SECONDS,
                2,
                1
        );

        System.out.println("# Submitting runnable tasks");
        for (int i = 1; i <= 10; i++) {
            pool.execute(new DemoTask("task-" + i, 3000));
        }

        System.out.println("# Submitting callable tasks");
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            int finalI = i;
            Future<String> future = pool.submit(() -> {
                String result = "callable-" + finalI + " done by " + Thread.currentThread().getName();
                Thread.sleep(1500);
                return result;
            });
            futures.add(future);
        }

        for (Future<String> future : futures) {
            System.out.println("[Main] Future result: " + future.get());
        }

        Thread.sleep(10000);

        System.out.println("# Calling shutdown()");
        pool.shutdown();

        Thread.sleep(3000);

        System.out.println("[Main] Worker count after shutdown: " + pool.getWorkerCount());
        System.out.println("[Main] Idle worker count after shutdown: " + pool.getIdleWorkerCount());

        System.out.println("# End of demo");
    }
}