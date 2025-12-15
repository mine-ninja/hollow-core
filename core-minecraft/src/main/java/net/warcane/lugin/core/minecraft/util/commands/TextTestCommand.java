package net.warcane.lugin.core.minecraft.util.commands;

import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Rok, Pedro Lucas nmm. Created on 24/07/2025
 * @project factions-essentials
 */
public class TextTestCommand extends SimpleCommand {

    public TextTestCommand() {
        super("text-test");
        setRequiredPermission("lugin.master");
        playersOnly = true;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        String font = ctx.getRawArgOrThrow(0, "Por favor, informe a fonte a ser testada.");

        if (ctx.getArgs().length < 1) {
            throw new CommandFailedException("Por favor, escreva um texto para testar a fonte.");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.getArgs().length; i++) {
            String arg = ctx.getArgs()[i];
            sb.append(arg);
            if (i < ctx.getArgs().length - 1) {
                sb.append(" ");
            }
        }

        StringUtils.send(ctx.getSenderAsPlayer(), sb.toString());
    }
}
