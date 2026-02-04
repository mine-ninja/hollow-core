package io.github.minehollow.mines.listener;

import io.github.minehollow.minecraft.util.PlayerUtil;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import io.github.minehollow.mines.MinesPlugin;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class BlockBreakListener implements Listener {

    private static final PredefinedSound NO_SPACE_SOUND = new PredefinedSound(Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
    private static final Component NO_SPACE_MESSAGE = StringUtils.formatString(
      "<red>Seu inventário está cheio! Esvazie seu inventário para coletar os itens."
    );

    private final MinesPlugin minesPlugin;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void handleMineBlockBreak(@NotNull BlockBreakEvent event) {
        final var player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        final var block = event.getBlock();
        final var mine = minesPlugin.getMineManager().getMineAt(block);
        if (mine == null) return;

        final var blockConfig = mine.getBlockConfigs().get(block.getType());
        if (blockConfig == null) {
            event.setCancelled(true);
            return;
        }

        double minExperienceReward = blockConfig.getMinExperienceReward();
        double maxExperienceReward = blockConfig.getMaxExperienceReward();

        blockConfig.getCurrencyValues().forEach((currency, value) -> {

        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void handleBlockEvent(@NotNull BlockBreakEvent event) {
        final var player = event.getPlayer();
        final var block = event.getBlock();

        event.setDropItems(false);

        final int experienceToDrop = event.getExpToDrop();
        if (experienceToDrop > 0) {
            player.giveExp(experienceToDrop);
            event.setExpToDrop(0);
        }

        final var playerItemInHand = player.getInventory().getItemInMainHand();
        final var dropsForThisItem = block.getDrops(playerItemInHand, player);
        if (dropsForThisItem.isEmpty()) {
            return;
        }

        for (var drop : dropsForThisItem) {
            if (!PlayerUtil.hasSpace(player, drop, drop.getAmount())) {
                player.sendMessage(NO_SPACE_MESSAGE);
                return;
            }

            player.getInventory().addItem(drop);
        }
    }


}
