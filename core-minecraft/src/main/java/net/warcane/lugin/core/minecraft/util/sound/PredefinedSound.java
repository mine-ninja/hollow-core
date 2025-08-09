package net.warcane.lugin.core.minecraft.util.sound;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;


public record PredefinedSound(@NotNull Sound sound, float volume, float pitch) {

    public void play(@NotNull Player player) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public void play(@NotNull Collection<? extends Player> players) {
        for (Player player : players) {
            play(player);
        }
    }
}
