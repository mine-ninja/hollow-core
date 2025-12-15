package net.warcane.lugin.core.proxy.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.warcane.lugin.core.proxy.VelocityPlatform;
import net.warcane.lugin.core.proxy.restart.RestartManager;
import net.warcane.lugin.core.server.type.ServerCategoryType;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Rok, Pedro Lucas nmm. Created on 03/12/2025
 * @project LUGIN
 */
public class RestartCommand {

    private static final SuggestionProvider<CommandSource> SERVER_CATEGORY_SUGGESTION = (ctx, builder) -> {
        for (ServerCategoryType category : ServerCategoryType.values()) {
            builder.suggest(category.name());
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> SERVER_NAME_SUGGESTION = (ctx, builder) -> {
        Set<String> suggestedServers = new HashSet<>();
        for (RegisteredServer allServer : ((VelocityPlatform) VelocityPlatform.getInstance()).getProxyServer().getAllServers()) {
            String serverName = allServer.getServerInfo().getName();
            if (suggestedServers.add(serverName)) {
                builder.suggest(serverName);
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> RESTART_ID_SUGGESTION = (ctx, builder) -> {
        RestartManager.get().getScheduledRestartIds().forEach(builder::suggest);
        return builder.buildFuture();
    };

    public static void register() {
        BrigadierCommand.literalArgumentBuilder("restart")
            .requires(source -> source.hasPermission("lugin.command.restart"))
            .then(BrigadierCommand.literalArgumentBuilder("stop")
                .then(BrigadierCommand.requiredArgumentBuilder("restart-id", IntegerArgumentType.integer(0))
                    .suggests(RESTART_ID_SUGGESTION)
                    .executes(RestartCommand::restartAllServers)))
            .then(BrigadierCommand.requiredArgumentBuilder("time", IntegerArgumentType.integer(10))
                .then(BrigadierCommand.literalArgumentBuilder("CATEGORY")
                    .then(BrigadierCommand.requiredArgumentBuilder("category", StringArgumentType.string())
                        .suggests(SERVER_CATEGORY_SUGGESTION)
                        .executes(RestartCommand::restartServerByCategory)
                    ))
                .then(BrigadierCommand.literalArgumentBuilder("ALL")
                    .executes(RestartCommand::restartAllServers))
                .then(BrigadierCommand.literalArgumentBuilder("SERVER")
                    .then(BrigadierCommand.requiredArgumentBuilder("serverName", StringArgumentType.string())
                        .suggests(SERVER_NAME_SUGGESTION)
                        .executes(RestartCommand::restartServerByName)
                    )));
        ;
    }


    private static int restartServerByCategory(CommandContext<CommandSource> ctx) {
        int time = IntegerArgumentType.getInteger(ctx, "time");

        return 1;
    }

    private static int restartServerByName(CommandContext<CommandSource> ctx) {
        int time = IntegerArgumentType.getInteger(ctx, "time");
        String serverName = StringArgumentType.getString(ctx, "serverName");

        return 1;
    }

    private static int restartAllServers(CommandContext<CommandSource> ctx) {
        int time = IntegerArgumentType.getInteger(ctx, "time");

        return 1;
    }
}
