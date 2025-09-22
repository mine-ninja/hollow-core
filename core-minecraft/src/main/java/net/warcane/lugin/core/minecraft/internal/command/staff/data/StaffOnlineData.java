package net.warcane.lugin.core.minecraft.internal.command.staff.data;

import net.warcane.lugin.core.group.PlayerGroup;

public record StaffOnlineData(String username, String serverId, PlayerGroup group) {
}
