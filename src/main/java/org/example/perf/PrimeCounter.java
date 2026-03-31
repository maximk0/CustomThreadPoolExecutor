package org.example.perf;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Function;

public final class PrimeCounter {

    private PrimeCounter() {
    }

    public static int countPrimes(
            Function<Callable<Integer>, Future<Integer>> submit, // (Callable<Int>) -> Future<Int>
            int fromInclusive,
            int toInclusive,
            int taskCount
    ) throws Exception {

        if (fromInclusive > toInclusive) {
            return 0;
        }
        if (taskCount <= 0) {
            throw new IllegalArgumentException("taskCount must be > 0");
        }

        List<Future<Integer>> futures = new ArrayList<>();

        int totalNumbers = toInclusive - fromInclusive + 1;
        int chunkSize = Math.max(1, totalNumbers / taskCount);

        int start = fromInclusive;
        while (start <= toInclusive) {
            int end = Math.min(start + chunkSize - 1, toInclusive);
            int finalStart = start;

            futures.add(submit.apply(() -> PrimeUtils.countPrimesInRange(finalStart, end)));

            start = end + 1;
        }

        int result = 0;
        for (Future<Integer> future : futures) {
            result += future.get();
        }
        return result;
    }
}