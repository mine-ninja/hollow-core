package io.github.minehollow.minecraft.util.stopwatch;

public class Stopwatch {

    private long start;

    public Stopwatch() {
        start = System.currentTimeMillis();
    }

    public boolean resetIfElapsed(long millis) {
        if (hasElapsed(millis)) {
            reset();
            return true;
        }
        return false;
    }

    public boolean resetIfElapsedSeconds(double seconds) {
        if (hasElapsedSeconds(seconds)) {
            reset();
            return true;
        }
        return false;
    }

    public boolean hasElapsed(long millis) {
        return elapsedTimeInMillis() >= millis;
    }

    public boolean hasElapsedSeconds(double seconds) {
        return elapsedTimeInSeconds() >= seconds;
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