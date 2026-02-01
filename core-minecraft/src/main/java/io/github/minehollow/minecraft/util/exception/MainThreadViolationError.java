package io.github.minehollow.minecraft.util.exception;

import org.bukkit.Bukkit;

public class MainThreadViolationError extends RuntimeException {

    public static void throwIfApplicable(String message) {
        if (Bukkit.isPrimaryThread()) {
            throw new MainThreadViolationError("Main Thread Violation: " + message);
        }
    }

    public static void throwIfApplicable() {
        throwIfApplicable("Operation not allowed on the main server thread.");
    }

    public MainThreadViolationError() {
    }

    public MainThreadViolationError(String message) {
        super(message);
    }

    public MainThreadViolationError(String message, Throwable cause) {
        super(message, cause);
    }

    public MainThreadViolationError(Throwable cause) {
        super(cause);
    }

    public MainThreadViolationError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
