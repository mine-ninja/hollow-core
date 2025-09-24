package net.warcane.lugin.core.minecraft.punish.command;

import net.kyori.adventure.audience.Audience;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.punish.api.RevokeManager;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;

import java.util.List;

/**
 * @author Rok, Pedro Lucas nmm. Created on 30/06/2025
 * @project punish
 */
public class RevokeCommand extends SimpleCommand {
    public RevokeCommand() {
        super("revoke");
        setRequiredPermission("lugin.moderator");
        setAliases(List.of("revogar"));
        this.playersOnly = true;
    }

    @Override
    public void performCommand(CommandContext commandContext) throws CommandFailedException {
        boolean isOnlyOneArg = commandContext.isArgsLength(1);
        if (!isOnlyOneArg && !commandContext.isArgsLength(2)) {
            throw new CommandFailedException("§cUso correto: /revoke <id>");
        }
        int punishmentId = commandContext.getIntOrDefault(0, -1);
        if (isOnlyOneArg) {
            RevokeManager.get().startRevokeSession(commandContext.getSenderAsPlayer(), punishmentId, null);
            return;
        }
        String reason = commandContext.getRawArgOrNull(1);
        RevokeManager.RevokeAction revokeAction = RevokeManager.RevokeAction.fromString(reason);
        if (revokeAction == null) {
            Audience audience = BukkitPlatform.getInstance().getAdventure().player(commandContext.getSenderAsPlayer());
            StringUtils.send(audience, "<l-red>Motivo de revogação inválido.");
            return;
        }
        RevokeManager.get().startRevokeSession(commandContext.getSenderAsPlayer(), punishmentId, revokeAction);
    }
}
