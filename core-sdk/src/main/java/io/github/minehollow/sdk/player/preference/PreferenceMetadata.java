package io.github.minehollow.sdk.player.preference;

import io.github.minehollow.sdk.player.account.PlayerAccount;

import java.util.List;
import java.util.function.BiConsumer;

public record PreferenceMetadata(
    String id,
    String name,
    List<String> description,
    boolean defaultValue,
    BiConsumer<PlayerAccount, Boolean> action
) {

    public void toggle(PlayerAccount account, boolean value) {
        action.accept(account, value);
    }
}
