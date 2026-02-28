package io.github.minehollow.essentials.tablist;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.protocol.score.ScoreFormat;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisplayScoreboard;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore;
import io.github.minehollow.minecraft.util.message.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Manages the below-name display for all players using PacketEvents
 * scoreboard packets.
 * <p>
 * Uses a scoreboard objective in the {@code BELOW_NAME} position (slot 2).
 * Suporta MiniMessage + PlaceholderAPI via placeholder resolver.
 * Requer Minecraft 1.20.3+ para ScoreFormat (fixedText).
 */
public class BelowNameManager {

    private static final String OBJECTIVE_NAME = "hc_below";

    private final PlayerManager packetPlayerManager;
    private final TabListPacketListener tabListPacketListener;
    private final String format;

    /** playerUUID → last resolved format string for dirty checking */
    private final Map<UUID, String> lastResolved = new ConcurrentHashMap<>();

    // Pre-built objective packets (immutable, reused)
    private final WrapperPlayServerScoreboardObjective createObjectivePacket;
    private final WrapperPlayServerDisplayScoreboard displayObjectivePacket;

    public BelowNameManager(@NotNull TabListPacketListener tabListPacketListener,
                            @NotNull String format) {
        this.packetPlayerManager = PacketEvents.getAPI().getPlayerManager();
        this.tabListPacketListener = tabListPacketListener;
        this.format = format;

        this.createObjectivePacket = new WrapperPlayServerScoreboardObjective(
            OBJECTIVE_NAME,
            WrapperPlayServerScoreboardObjective.ObjectiveMode.CREATE,
            Component.empty(),
            WrapperPlayServerScoreboardObjective.RenderType.INTEGER
        );

        this.displayObjectivePacket = new WrapperPlayServerDisplayScoreboard(
            2, // slot BELOW_NAME
            OBJECTIVE_NAME
        );
    }

    // ── Join / Quit ──────────────────────────────────────────

    public void onJoin(@NotNull Player player,
                       @NotNull Collection<? extends Player> onlinePlayers,
                       @NotNull BiFunction<String, Player, String> placeholderResolver) {
        // Cria o objetivo no client do jogador que entrou
        packetPlayerManager.sendPacket(player, createObjectivePacket);
        packetPlayerManager.sendPacket(player, displayObjectivePacket);

        // Envia todos os scores existentes para o jogador que entrou
        for (Player online : onlinePlayers) {
            if (online.equals(player)) continue;

            String tabUsername = tabListPacketListener.getTablistUsername(online.getUniqueId());
            String cached = lastResolved.get(online.getUniqueId());
            if (tabUsername == null || cached == null) continue;

            packetPlayerManager.sendPacket(player, buildScorePacket(tabUsername, cached));
        }

        // Resolve e broadcast o score do jogador que entrou para todos
        String joinerTab = tabListPacketListener.getTablistUsername(player.getUniqueId());
        if (joinerTab != null) {
            String resolved = placeholderResolver.apply(format, player);
            lastResolved.put(player.getUniqueId(), resolved);
            WrapperPlayServerUpdateScore scorePacket = buildScorePacket(joinerTab, resolved);
            for (Player online : onlinePlayers) {
                packetPlayerManager.sendPacket(online, scorePacket);
            }
        }
    }

    public void onQuit(@NotNull Player player) {
        lastResolved.remove(player.getUniqueId());

        String tabUsername = tabListPacketListener.getTablistUsername(player.getUniqueId());
        if (tabUsername != null) {
            WrapperPlayServerUpdateScore removeScore = new WrapperPlayServerUpdateScore(
                tabUsername,
                WrapperPlayServerUpdateScore.Action.REMOVE_ITEM,
                OBJECTIVE_NAME,
                Optional.empty()
            );
            for (Player online : Bukkit.getOnlinePlayers()) {
                packetPlayerManager.sendPacket(online, removeScore);
            }
        }
    }

    // ── Tick update ──────────────────────────────────────────

    /**
     * Atualiza o below-name de um jogador.
     * Só envia pacotes quando o formato resolvido realmente mudou (dirty check).
     */
    public void update(@NotNull Player player,
                       @NotNull BiFunction<String, Player, String> placeholderResolver) {
        UUID uuid = player.getUniqueId();
        String tabUsername = tabListPacketListener.getTablistUsername(uuid);
        if (tabUsername == null) return;

        String resolved = placeholderResolver.apply(format, player);

        // Dirty check — skip se não mudou nada
        String last = lastResolved.get(uuid);
        if (resolved.equals(last)) return;

        lastResolved.put(uuid, resolved);

        WrapperPlayServerUpdateScore scorePacket = buildScorePacket(tabUsername, resolved);
        for (Player online : Bukkit.getOnlinePlayers()) {
            packetPlayerManager.sendPacket(online, scorePacket);
        }
    }

    // ── Internals ────────────────────────────────────────────

    /**
     * Monta o pacote UPDATE_SCORE usando {@link ScoreFormat#fixedScore(Component)}.
     * <p>
     * O {@code ScoreFormat.fixedScore} substitui completamente o número exibido
     * abaixo do nome pelo Component fornecido, permitindo exibir qualquer texto
     * colorido/formatado via MiniMessage.
     *
     * @param tabUsername    nome do jogador conforme registrado na tablist
     * @param resolvedFormat string já resolvida (placeholders aplicados)
     */
    private static @NotNull WrapperPlayServerUpdateScore buildScorePacket(
        @NotNull String tabUsername,
        @NotNull String resolvedFormat
    ) {
        Component display = StringUtils.formatString(resolvedFormat);

        return new WrapperPlayServerUpdateScore(
            tabUsername,
            WrapperPlayServerUpdateScore.Action.CREATE_OR_UPDATE_ITEM,
            OBJECTIVE_NAME,
            0,                              // score numérico (irrelevante com ScoreFormat fixedText)
            null,                           // entityDisplayName (não utilizado aqui)
            ScoreFormat.fixedScore(display) // substitui o número pelo Component formatado
        );
    }
}