package net.warcane.lugin.core.proxy.restart;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Rok, Pedro Lucas nmm. Created on 03/12/2025
 * @project LUGIN
 */
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
