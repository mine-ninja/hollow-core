package io.github.minehollow.sdk.util;

import java.util.function.Supplier;

public class Lazy<T> {
    private volatile T value;
    private final Supplier<T> supplier;

    private Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public static <T> Lazy<T> of(Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    public T get() {
        if (value == null) {
            synchronized (this) {
                if (value == null) {
                    value = supplier.get();
                }
            }
        }
        return value;
    }

    public void reset() {
        synchronized (this) {
            value = null;
        }
    }

    public boolean isInitialized() {
        return value != null;
    }
}