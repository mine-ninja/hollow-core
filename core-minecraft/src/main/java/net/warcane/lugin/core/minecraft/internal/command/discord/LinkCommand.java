package net.warcane.lugin.core.minecraft.internal.command.discord;

import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.util.message.ComponentBuilder;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class LinkCommand extends SimpleCommand {

    @NotNull
    private final BukkitPlatform platform;

    public LinkCommand(@NotNull BukkitPlatform platform) {
        super("vincular");
        this.platform = platform;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        var player = ctx.getSenderAsPlayer();
        var playerUniqueId = player.getUniqueId();
        platform.getPlayerDiscordService().getPlayerDiscord(playerUniqueId).whenCompleteAsync((playerDiscord, throwable) -> {
            if (throwable != null) {
                log.error("Failed to get player discord for {} during link account discord: {}", player.getName(), throwable.getMessage(), throwable);
                player.sendMessage("§cOcorreu um erro ao tentar recuperar sua conta do Discord. Tente novamente mais tarde.");
                return;
            }

            if (playerDiscord != null && playerDiscord.isLinked()) {
                player.sendMessage("§cSua conta já está vinculada ao Discord.");
                return;
            }

            var code = platform.getDiscordService().createNewCode(player.getUniqueId());
            var audience = BukkitPlatformPlugin.getInstance().adventure().player(player);

            new ComponentBuilder()
                .simple("§b§lVINCULAÇÃO COM O DISCORD")
                .newLine()
                .newLine()
                .simple(String.format("§7Olá, §e%s§7! Use o código abaixo no nosso Discord.", player.getName()))
                .newLine()
                .newLine()
                .simple("§7Seu código (válido por 5 minutos):")
                .newLine()
                .clipboardHover(String.format("§a              %s", code), code, "§7Clique para copiar o código")
                .newLine()
                .newLine()
                .simple("§7Siga os passos do canal: §dVincular §7no nosso Discord.")
                .newLine()
                .send(audience);
        });
    }
}
