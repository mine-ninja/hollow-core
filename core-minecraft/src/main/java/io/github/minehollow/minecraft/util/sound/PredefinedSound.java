package io.github.minehollow.minecraft.util.sound;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import java.util.Collection;

public record PredefinedSound(@NotNull Sound sound, float volume, float pitch) {
    public void play(@NotNull Player player) {
        player.playSound(net.kyori.adventure.sound.Sound.sound(sound, net.kyori.adventure.sound.Sound.Source.MASTER, volume, pitch), net.kyori.adventure.sound.Sound.Emitter.self());
    }
    
    public void play(@NotNull Collection<? extends Player> players) {
        for (Player player : players) {
            play(player);
        }
    }
}
