package org.example.perf;

public final class PrimeUtils {

    private PrimeUtils() {
    }

    public static boolean isPrime(int n) {
        if (n < 2) {
            return false;
        }
        if (n == 2) {
            return true;
        }
        if (n % 2 == 0) {
            return false;
        }

        int limit = (int) Math.sqrt(n);
        for (int i = 3; i <= limit; i += 2) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }

    public static int countPrimesInRange(int fromInclusive, int toInclusive) {
        int count = 0;
        for (int i = fromInclusive; i <= toInclusive; i++) {
            if (isPrime(i)) {
                count++;
            }
        }
        return count;
    }
}