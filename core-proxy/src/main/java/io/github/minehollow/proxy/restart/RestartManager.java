package io.github.minehollow.proxy.restart;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class RestartManager {

    private final static RestartManager INSTANCE = new RestartManager();

    private final Map<Integer, RestartData> scheduledRestarts;

    public RestartManager() {
        scheduledRestarts = new HashMap<>();
    }

    public Set<Integer> getScheduledRestartIds() {
        return scheduledRestarts.keySet();
    }

    public static RestartManager get() {
        return INSTANCE;
    }
}
