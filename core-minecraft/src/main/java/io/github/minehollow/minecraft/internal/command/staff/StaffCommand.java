package io.github.minehollow.minecraft.internal.command.staff;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.internal.command.staff.data.StaffOnlineData;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.sdk.util.data.RedisCache;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StaffCommand extends SimpleCommand {

    private final RedisCache<StaffOnlineData> staffOnlineCache = new RedisCache<>(StaffOnlineData.class);

    private final BukkitPlatform platform;

    public StaffCommand(BukkitPlatform platform) {
        super("staff");
        this.setAliases(List.of("equipe"));
        this.setRequiredPermission("hollow.staff");
        this.platform = platform;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var player = ctx.getSenderAsPlayer();

        Tasks.runAsync(() -> {
            var staffList = staffOnlineCache.hgetAll("staff_online").stream()
                .sorted((s1, s2) -> Integer.compare(s2.group().getPowerLevel(), s1.group().getPowerLevel()))
                .toList();

            if (staffList.isEmpty()) {
                player.sendMessage("Nenhum membro da equipe está online no momento.");
                return;
            }

            player.sendMessage("\n§fLista de membros da equipe online:\n");
            staffList.forEach(staff -> player.sendMessage(
                String.format("  §f- %s%s §f(%s)",
                    staff.group().getPrefix(),
                    staff.username(),
                    formatServerName(staff.serverId())
                )
            ));
            player.sendMessage("\n");
        });
    }

    private String formatServerName(String serverId) {
        var server = platform.getGameServerService().getById(serverId);
        if (server == null) {
            return "";
        }

        return serverId + " - " + server.categoryType().getDisplayName();
    }
}
