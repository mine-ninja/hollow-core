package io.github.minehollow.minecraft.util.sound;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class PredefinedSoundGroups {

    // --- DOPAMINA INSTANTÂNEA (FEEDBACK DE LOOP) ---

    // Venda de Item: Pitch progressivo dá sensação de "empilhamento" de dinheiro
    public static final PredefinedSound SELL_ITEM = new PredefinedSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);

    // Rankup: Explosão tripla (Poder, Sucesso e Brilho)
    public static final List<PredefinedSound> RANK_UP = List.of(
        new PredefinedSound(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f),
        new PredefinedSound(Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.5f),
        new PredefinedSound(Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f)
    );

    // Drop Raro: Som de "Cristal" (Longo e agudo para destacar do resto)
    public static final List<PredefinedSound> RARE_DROP = List.of(
        new PredefinedSound(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f),
        new PredefinedSound(Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.8f),
        new PredefinedSound(Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.4f, 2.0f)
    );

    // --- MELODIAS RPG (COMPLEXAS) ---

    /**
     * Fanfarra de Vitória RPG: Uma melodia heróica e rápida. Ideal para: Concluir Masmorras ou atingir Prestígio.
     */
    public static void playAdventureVictory(@NotNull Plugin plugin, @NotNull Player player) {
        final var atomicTicker = new AtomicInteger(0);
        // Melodia em escala ascendente (C-E-G-C) para sensação de triunfo
        List<PredefinedSound> sequence = List.of(
            new PredefinedSound(Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 0.75f), // Dó
            new PredefinedSound(Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 0.95f), // Mi
            new PredefinedSound(Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.15f), // Sol
            new PredefinedSound(Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.50f), // Dó (Oitava)
            new PredefinedSound(Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.2f) // Brilho Final
        );

        Bukkit.getAsyncScheduler().runAtFixedRate(
            plugin, task -> {
                int index = atomicTicker.getAndIncrement();
                if (index < sequence.size()) {
                    sequence.get(index).play(player);
                } else {
                    task.cancel();
                }
            }, 0, 150L, TimeUnit.MILLISECONDS
        );
    }

    /**
     * Jackpot: Som de "Slot Machine" (Cassino) Use quando o jogador ganhar algo muito valioso em uma Crate.
     */
    public static void playJackpot(@NotNull Plugin plugin, @NotNull Player player) {
        final var atomicTicker = new AtomicInteger(0);
        Bukkit.getAsyncScheduler().runAtFixedRate(
            plugin, task -> {
                int count = atomicTicker.getAndIncrement();
                if (count < 6) {
                    new PredefinedSound(Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.5f + (count * 0.1f)).play(player);
                } else {
                    new PredefinedSound(Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f).play(player);
                    new PredefinedSound(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f).play(player);
                    task.cancel();
                }
            }, 0, 100L, TimeUnit.MILLISECONDS
        );
    }

    // --- COMBATE E UTILITÁRIOS ---

    // Crítico: Som de impacto pesado e "limpo"
    public static final List<PredefinedSound> CRITICAL_HIT = List.of(
        new PredefinedSound(Sound.ENTITY_IRON_GOLEM_DAMAGE, 0.5f, 1.5f),
        new PredefinedSound(Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f)
    );

    // Quest Completada: Flauta suave seguida de sino (Desejo de "Quero mais uma")
    public static final List<PredefinedSound> QUEST_COMPLETE = List.of(
        new PredefinedSound(Sound.BLOCK_NOTE_BLOCK_FLUTE, 1.0f, 1.2f),
        new PredefinedSound(Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.5f)
    );

    // Erro / Negado: Som grave e curto (Indica que ele precisa de mais recursos)
    public static final PredefinedSound ERROR = new PredefinedSound(Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 0.6f);

    public static void playGroup(@NotNull Player player, @NotNull List<PredefinedSound> group) {
        group.forEach(s -> s.play(player));
    }
}