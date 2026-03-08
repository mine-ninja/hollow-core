package io.github.minehollow.essentials.command;

import io.github.minehollow.essentials.EssentialsPlugin;
import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.util.message.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TpaCommand extends SimpleCommand {

    private final EssentialsPlugin plugin;

    public TpaCommand(@NotNull EssentialsPlugin plugin) {
        super("tpa", "hollow.tpa");
        this.plugin = plugin;
        this.playersOnly = true;
    }

    private MessageConfig msg() { return plugin.getMessageConfig(); }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player sender = ctx.getSenderAsPlayer();
        String targetName = ctx.getRawArgOrThrow(0, msg().get("invalid-usage", "usage", "/tpa <player>"));

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            msg().send(sender, "player-not-found", "player", targetName);
            return;
        }

        if (target.getUniqueId().equals(sender.getUniqueId())) {
            throw new CommandFailedException(msg().get("invalid-usage", "usage", "/tpa <player>"));
        }

        boolean created = plugin.getTpaService().createRequest(sender.getUniqueId(), target.getUniqueId());
        if (!created) {
            msg().send(sender, "tpa-already-pending", "target", target.getName());
            return;
        }

        msg().send(sender, "tpa-sent", "target", target.getName());

        List<String> receivedLines = msg().getList("tpa-received", "player", sender.getName());
        for(final var msg : receivedLines){
            var component = StringUtils.formatString(msg)
                .replaceText(StringUtils.replaceComponent("@accept_button@", this.createAcceptButton(sender)))
                .replaceText(StringUtils.replaceComponent("@deny_button@", this.createDenyButton(sender)));

            target.sendMessage(component);
        }


        //var receivedComponent = StringUtils.multiText(receivedLines.toArray(new String[0]))
        //    .replaceText(StringUtils.replaceComponent("{accept_button}", this.createAcceptButton(sender)))
        //    .replaceText(StringUtils.replaceComponent("{deny_button}", this.createDenyButton(sender)));

        //target.sendMessage(receivedComponent);
    }

    private Component createAcceptButton(Player sender) {
        return StringUtils.formatString("<green><b>ACEITAR</b>")
            .clickEvent(ClickEvent.runCommand("/tpaccept " + sender.getUniqueId()))
            .hoverEvent(HoverEvent.showText(StringUtils.formatString("<green>Clique para aceitar o pedido de teleporte.")));
    }

    private Component createDenyButton(Player sender) {
        return StringUtils.formatString("<red><b>NEGAR</b>")
            .clickEvent(ClickEvent.runCommand("/tpdeny"))
            .hoverEvent(HoverEvent.showText(StringUtils.formatString("<red>Clique para negar o pedido de teleporte.")));
    }

    @Override
    public List<String> performTabComplete(CommandContext ctx) {
        if (ctx.getArgs().length == 0) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (ctx.getArgs().length == 1) {
            String prefix = ctx.getArgs()[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .toList();
        }
        return List.of();
    }
}
