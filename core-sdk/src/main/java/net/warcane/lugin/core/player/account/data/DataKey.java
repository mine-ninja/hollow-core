package net.warcane.lugin.core.player.account.data;

import com.google.gson.reflect.TypeToken;

public record DataKey<T>(
    String id,
    TypeToken<T> type
) {

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DataKey<?> dataKey)) return false;

        return id.equals(dataKey.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
