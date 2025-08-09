package net.warcane.lugin.core.minecraft.util.stopwatch;

public class Stopwatch {

    private long start;

    public Stopwatch() {
        start = System.currentTimeMillis();
    }

    public long elapsedTimeInMillis() {
        return System.currentTimeMillis() - start;
    }

    public double elapsedTimeInSeconds() {
        long now = System.currentTimeMillis();
        return (now - start) / 1000.0;
    }

    public void reset() {
        start = System.currentTimeMillis();
    }
}