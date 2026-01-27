package io.github.minehollow.minecraft.internal.command.test;

import lombok.extern.slf4j.Slf4j;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.menu.MenuContext;
import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TestMenuCommand extends SimpleCommand {

    private final BukkitPlatform platform;

    public TestMenuCommand(BukkitPlatform platform) {
        super("testMenu", "hollow.master");
        this.platform = platform;
        platform.getMenuManager().register(new TestMenu());
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        platform.getMenuManager().openToPlayer(ctx.getSenderAsPlayer(), TestMenu.class);
    }

    public static class TestMenu extends SimpleMenu {

        private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();
        private static final List<Material> TEST_MATERIALS = List.of(
          Material.DIAMOND_SWORD, Material.GOLDEN_APPLE, Material.IRON_PICKAXE,
          Material.BOW, Material.ARROW, Material.COOKED_BEEF
        );

        private ItemStack randomItem() {
            Material material = TEST_MATERIALS.get(RANDOM.nextInt(TEST_MATERIALS.size()));
            return new ItemStack(material, RANDOM.nextInt(1, 5));
        }

        @Override
        public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig openHandler) {
            Player player = ctx.getPlayer();
            if (!player.hasPermission("hollow.master")) {
                return false; // nao vai abrir o menu se retornar 'false'
            }

            openHandler.setTickUpdateEnabled(true);
            openHandler.setUpdateIntervalMillis(3, TimeUnit.SECONDS); // a cada 3 segundos
            openHandler.setTitle("Test Menu");
            openHandler.setRows(3);

            openHandler.setClickSound(new PredefinedSound(Sound.BLOCK_LEVER_CLICK, 1, 1));
            openHandler.setCloseSound(new PredefinedSound(Sound.BLOCK_IRON_DOOR_CLOSE, 1, 1));

            ctx.setItem(ctx.getBorders(), p -> randomItem());


            return true; // vai abrir o menu se retornar 'true'
        }

        @Override
        protected void onTick(@NotNull MenuContext ctx) {
            ctx.update();
            super.onTick(ctx);
        }

        @Override
        protected void onError(@NotNull PlayerMenuContext ctx, @Nullable InventoryEvent event, @NotNull Throwable error) {
            final var eventName = event.getEventName();
            log.error("Erro no menu {} durante o evento {}", this.getClass().getSimpleName(), eventName, error);
        }
    }
}
