package io.github.minehollow.minecraft.util.message.input;

import io.github.minehollow.minecraft.BukkitPlatformPlugin;
import io.github.minehollow.minecraft.util.message.StringUtils;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;


public record InputSession(UUID uuid, Consumer<String> inputSupplier, long expireIn, Consumer<Player> whenCancelled,
                           Consumer<Player> whenExpire) {

    public static InputSession of(UUID uuid, Consumer<String> inputSupplier) {
        return of(uuid, inputSupplier, System.currentTimeMillis() + 30000);
    }

    public static InputSession of(UUID uuid, Consumer<String> inputSupplier, long expireIn) {
        return new InputSession(uuid, inputSupplier, expireIn, (player) -> {
            ;
            final var audience = BukkitPlatformPlugin.getInstance().adventure().player(player);
            StringUtils.send(player, "<l-negate>Sua ação foi cancelada.");
        }, (player) -> {
            final var audience = BukkitPlatformPlugin.getInstance().adventure().player(player);
            StringUtils.send(player, "<l-negate>Sua ação foi cancelada.");
        });
    }

    public static InputSession of(UUID uuid, Consumer<String> inputSupplier, long expireIn, Consumer<Player> whenCancelled, Consumer<Player> whenExpire) {
        return new InputSession(uuid, inputSupplier, expireIn, whenCancelled, whenExpire);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireIn;
    }
}
