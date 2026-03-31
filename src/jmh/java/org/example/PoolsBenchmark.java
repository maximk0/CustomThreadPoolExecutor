package org.example;

import org.example.perf.PrimeCounter;
import org.example.reject_policy.CustomCallerRunsPolicy;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class PoolsBenchmark {

    @Param({"10000"})
    public int range;

    @Param({"64"})
    public int tasks;

    @Param({"8"})
    public int threads;

    private ThreadPoolExecutor threadPoolExecutor;
    private ForkJoinPool forkJoinPool;
    private CustomThreadPoolExecutor customThreadPoolExecutor;

    @Setup(Level.Iteration)
    public void setUp() {
        threadPoolExecutor = new ThreadPoolExecutor(
                threads,
                threads,
                5L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );

        forkJoinPool = new ForkJoinPool(threads);

        customThreadPoolExecutor = new CustomThreadPoolExecutor(
                threads,
                threads,
                5L,
                TimeUnit.SECONDS,
                1024,
                0,
                new CustomCallerRunsPolicy(),
                "BenchmarkPool",
                false
        );
    }

    @TearDown(Level.Iteration)
    public void tearDown() throws InterruptedException {
        threadPoolExecutor.shutdownNow();
        forkJoinPool.shutdownNow();
        customThreadPoolExecutor.shutdownNow();

        threadPoolExecutor.awaitTermination(1, TimeUnit.MINUTES);
        forkJoinPool.awaitTermination(1, TimeUnit.MINUTES);
        customThreadPoolExecutor.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Benchmark
    public long threadPoolExecutorBenchmark() throws Exception {
        return PrimeCounter.countPrimes(
                threadPoolExecutor::submit,
                1,
                range,
                tasks
        );
    }

    @Benchmark
    public long forkJoinPoolBenchmark() throws Exception {
        int threshold = Math.max(1, range / tasks);
        return PrimeCounter.countPrimes(
                forkJoinPool::submit,
                1,
                range,
                threshold
        );
    }

    @Benchmark
    public long customPoolBenchmark() throws Exception {
        return PrimeCounter.countPrimes(
                customThreadPoolExecutor::submit,
                1,
                range,
                tasks
        );
    }
}
