package net.warcane.lugin.core.cooldown;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public interface Cooldown<T> {

    default void insert(T o, long time, TimeUnit timeUnit) {
        insert(o, timeUnit.toMillis(time));
    }

    default void insert(T o, Duration duration) {
        insert(o, duration.toMillis());
    }

    void insert(T t, long durationMillis);

    long getReamingMillis(T o);

    default int getReamingSeconds(T t) {
        return (int) Math.max(0, getReamingMillis(t) / 1000);
    }

    default String getReamingSecondsFormatted(T t) {
        long seconds = getReamingMillis(t);
        return seconds == 1 ? seconds + " segundo" : UtilTime.simpleFormatSSMS(seconds) + " segundos";
    }

    boolean isWaiting(T t);

    void remove(T t);

    void invalidate();

}
