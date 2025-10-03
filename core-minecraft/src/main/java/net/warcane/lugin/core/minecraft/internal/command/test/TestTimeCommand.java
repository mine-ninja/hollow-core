package net.warcane.lugin.core.minecraft.internal.command.test;

import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.util.time.Time;

import org.jetbrains.annotations.NotNull;

public class TestTimeCommand extends SimpleCommand {
    public TestTimeCommand() {
        super("testTime", "lugin.master");
    }
    
    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var time = parseTime(ctx.getRawArgOrThrow(0, "Informe uma duração de tempo válida: '1d', '2h', '3m', etc."));
        ctx.sendMessage("§aTempo formatado: " + Time.formatInstant(time.toInstantFromNow()));
    }
    
    private Time parseTime(@NotNull String input) {
        try {
            return Time.parseString(input);
        } catch (IllegalArgumentException e) {
            throw new CommandFailedException("Invalid time format. Use '1d', '2h', '3m', etc.");
        }
    }
}
