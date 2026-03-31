package org.example.demo;

public class DemoTask implements Runnable {
    private final String name;
    private final long durationMillis;

    public DemoTask(String name, long durationMillis) {
        this.name = name;
        this.durationMillis = durationMillis;
    }

    @Override
    public void run() {
        System.out.println("[Task] " + name + " started on " + Thread.currentThread().getName());
        try {
            Thread.sleep(durationMillis);
        } catch (InterruptedException e) {
            System.out.println("[Task] " + name + " interrupted on " + Thread.currentThread().getName());
            Thread.currentThread().interrupt();
        }
        System.out.println("[Task] " + name + " finished on " + Thread.currentThread().getName());
    }

    @Override
    public String toString() {
        return "DemoTask{name='" + name + "', durationMillis=" + durationMillis + "}";
    }
}