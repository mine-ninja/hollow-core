package io.github.minehollow.lobby.menu;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ServerMenu extends SimpleMenu {


    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig openHandler) {

        openHandler.setTitle("Servidores");
        openHandler.setRows(3);

        ctx.setItem(13, this.generateRankupItem(), click -> {
            final var player = (Player) click.getWhoClicked();
            BukkitPlatform.getInstance().tryConnectPlayerToServer(player.getUniqueId(), "rankup01");
        });

        return true;
    }


    private ItemStack generateRankupItem() {
        final var server = BukkitPlatform.getInstance()
          .getGameServerService()
          .getById("rankup01");

        var onlinePlayerCount = 0;
        if (server != null) {
            onlinePlayerCount = server.serverPlayers().online();
        }


        return ItemBuilder.skull()
          .name("<gradient:#9D4EDD:#C77DFF:#9D4EDD>RANKUP Hollow</gradient>")
          .lore(
            "<gray>" + onlinePlayerCount + " jogadores online",
            "",
            "<gray>Junte-se ao nosso servidor de rankup,",
            "<gray>suba de nível e desbloqueie recompensas exclusivas!",
            "",
            "<yellow>Clique para entrar!"
          )
          .build();
    }
}
