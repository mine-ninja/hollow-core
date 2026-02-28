package io.github.minehollow.npc.api.actions;

import io.github.minehollow.npc.api.Npc;
import io.github.minehollow.npc.api.NpcAction;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Plays a sound to the clicking player.
 */
public class SoundAction implements NpcAction {

    private final Sound sound;
    private final float volume;
    private final float pitch;

    public SoundAction(@NotNull Sound sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void execute(@NotNull Player player, @NotNull Npc npc) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    @Override
    public @NotNull String getType() {
        return "SOUND";
    }

    public @NotNull Sound getSound() {
        return sound;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }
}

