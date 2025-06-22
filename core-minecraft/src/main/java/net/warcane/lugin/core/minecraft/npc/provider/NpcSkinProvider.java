package net.warcane.lugin.core.minecraft.npc.provider;

import net.warcane.lugin.core.minecraft.npc.Npc;
import net.warcane.lugin.core.minecraft.skin.Skin;
import net.warcane.lugin.core.minecraft.skin.SkinFetcher;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@FunctionalInterface
public interface NpcSkinProvider {

    static NpcSkinProvider fromSkin(@NotNull Skin skin) {
        return (npc, viewer) -> skin;
    }

    static NpcSkinProvider fromPlayerName(@NotNull String playerName) {
        return (npc, viewer) -> SkinFetcher.INSTANCE.fetchByUsername(playerName);
    }

    static NpcSkinProvider fromPlayerUniqueId(@NotNull UUID uniqueId) {
        return (npc, viewer) -> SkinFetcher.INSTANCE.fetchByUniqueId(uniqueId);
    }

    @Nullable Skin provideSkin(@NotNull Npc npc, @NotNull Player viewer);
}
