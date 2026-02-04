package io.github.minehollow.mines.listener;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.util.PlayerUtil;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import io.github.minehollow.mines.MinesPlugin;
import io.github.minehollow.mines.filler.MineFiller;
import io.github.minehollow.sdk.player.wallet.Wallet;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public class BlockBreakListener implements Listener {

    private static final PredefinedSound NO_SPACE_SOUND = new PredefinedSound(Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
    private static final Component NO_SPACE_MESSAGE = StringUtils.formatString(
      "<red>Seu inventário está cheio! Esvazie seu inventário para coletar os itens."
    );

    private final MinesPlugin minesPlugin;

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void handleMineBlockBreak(@NotNull BlockBreakEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        final Block block = event.getBlock();

        if (!minesPlugin.getMineManager().getMineWorldName().equalsIgnoreCase(block.getWorld().getName())) {
            return;
        }

        final var mine = minesPlugin.getMineManager().getMineAt(block.getX(), block.getY(), block.getZ());
        if (mine == null) return;

        final var blockConfig = mine.getBlockConfigs().get(block.getType());
        if (blockConfig == null) {
            event.setCancelled(true);
            return;
        }

        mine.incrementAirBlocks(1);
        mine.updateLastBlockBreakTime();

        final ThreadLocalRandom random = ThreadLocalRandom.current();

        double minExp = blockConfig.getMinExperienceReward();
        double maxExp = blockConfig.getMaxExperienceReward();

        if (maxExp > 0) {
            double experienceGain = random.nextDouble(minExp, maxExp);
            if (experienceGain > 0) {
                event.setExpToDrop((int) (experienceGain + 0.5));
                player.playSound(player.getLocation(),
                  Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                  0.7f + random.nextFloat() * 0.3f,
                  0.9f + random.nextFloat() * 0.2f
                );
            }
        }

        final var platform = BukkitPlatform.getInstance();
        final Wallet playerWallet = platform.getWalletService().getCachedWalletOrThrow(player.getUniqueId());
        final var currencyManager = platform.getCurrencyManager();

        for (final var entry : blockConfig.getCurrencyValues().entrySet()) {
            if (currencyManager.getCurrency(entry.getKey()) != null) {
                playerWallet.incrementCurrencyAmount(entry.getKey(), BigDecimal.valueOf(entry.getValue()));
            }
        }

        if (mine.canReset()) {
            MineFiller.fillMine(minesPlugin.getMineManager(), mine, null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void handleBlockEvent(@NotNull BlockBreakEvent event) {
        final int exp = event.getExpToDrop();
        final Player player = event.getPlayer();

        event.setDropItems(false);

        if (exp > 0) {
            player.giveExp(exp);
            event.setExpToDrop(0);
        }

        final var block = event.getBlock();
        final var tool = player.getInventory().getItemInMainHand();
        final var drops = block.getDrops(tool, player);

        if (drops.isEmpty()) return;

        for (ItemStack drop : drops) {
            int amount = drop.getAmount();
            if (!PlayerUtil.hasSpace(player, drop, amount)) {
                player.sendMessage(NO_SPACE_MESSAGE);
                NO_SPACE_SOUND.play(player);
                return;
            }
            player.getInventory().addItem(drop);
        }
    }
}