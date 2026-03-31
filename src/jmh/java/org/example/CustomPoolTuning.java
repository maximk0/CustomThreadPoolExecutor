package org.example;

import org.example.perf.PrimeCounter;
import org.example.reject_policy.CustomCallerRunsPolicy;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class CustomPoolTuning {

    public int range = 10_000;

    @Param({"8", "32", "64"})
    public int tasks;

    @Param({"12", "16", "20"})
    public int threads;

    @Param({"4", "32", "128"})
    public int queueSize;

    @Param({"0", "1", "5"})
    public int minSpareThreads;

    private CustomThreadPoolExecutor customThreadPoolExecutor;

    @Setup(Level.Iteration)
    public void setUp() {
        customThreadPoolExecutor = new CustomThreadPoolExecutor(
                threads,
                threads,
                5L,
                TimeUnit.SECONDS,
                queueSize,
                minSpareThreads,
                new CustomCallerRunsPolicy(),
                "BenchmarkPool",
                false
        );
    }

    @TearDown(Level.Iteration)
    public void tearDown() throws InterruptedException {
        customThreadPoolExecutor.shutdownNow();

        customThreadPoolExecutor.awaitTermination(1, TimeUnit.MINUTES);
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