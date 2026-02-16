package io.github.minehollow.lobby.listener;

import io.github.minehollow.lobby.LobbyPlugin;
import io.github.minehollow.lobby.hologram.HologramManager;
import io.github.minehollow.lobby.npc.NPCManager;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerJoinListener implements Listener {

    private final PredefinedSound JOIN_SOUND = new PredefinedSound(
      Sound.ENTITY_PLAYER_LEVELUP,
      1.0f,
      1.0f
    );

    private static final Title WELCOME_TITLE = Title.title(
      StringUtils.formatString("<gradient:#9D4EDD:#C77DFF:#9D4EDD>MineHollow"),
      StringUtils.formatString("<white>Bem-vindo!")
    );


    private final LobbyPlugin plugin;
    private final HologramManager hologramManager;
    private final NPCManager npcManager;

    public PlayerJoinListener(@NotNull LobbyPlugin plugin, HologramManager hologramManager, NPCManager npcManager) {
        this.plugin = plugin;
        this.hologramManager = hologramManager;
        this.npcManager = npcManager;
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent e) {
        var player = e.getPlayer();
        hologramManager.addViewerToAll(player);
        npcManager.addViewerToAll(player);

        player.teleport(plugin.getSpawnLocation());

        player.showTitle(WELCOME_TITLE);
        JOIN_SOUND.play(player);



        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setAbsorptionAmount(0.0);
        player.setExp(0.0f);
        player.setLevel(0);
        player.setFireTicks(0);
        player.setRemainingAir(player.getMaximumAir());

        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(true);

        var inventory = player.getInventory();
        inventory.clear();
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        e.joinMessage(null);

        player.getInventory().setItem(4, LobbyPlugin.SERVER_SELECTOR);
    }

}