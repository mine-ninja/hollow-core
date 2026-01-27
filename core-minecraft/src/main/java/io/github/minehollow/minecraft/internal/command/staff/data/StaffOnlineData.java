package io.github.minehollow.minecraft.internal.command.staff.data;

import io.github.minehollow.sdk.group.PlayerGroup;

public record StaffOnlineData(String username, String serverId, PlayerGroup group) {
}
