package net.warcane.lugin.core.minecraft.npc;


import net.warcane.lugin.core.minecraft.npc.interact.NpcInteractListener;
import net.warcane.lugin.core.minecraft.npc.provider.NpcSkinProvider;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class NpcBuilder {

    private final Location location;

    private NpcInteractListener interactListener;
    private NpcSkinProvider skinProvider;

    private Predicate<Player> renderFilter = player -> true;
    private final Map<String, Object> metadata = new HashMap<>();

    public NpcBuilder(Location location) {
        this.location = location;
    }

    public NpcBuilder withRenderFilter(@NotNull Predicate<Player> renderFilter) {
        this.renderFilter = renderFilter;
        return this;
    }

    public NpcBuilder withInteractListener(@NotNull NpcInteractListener interactListener) {
        this.interactListener = interactListener;
        return this;
    }

    public NpcBuilder withSkinProvider(@NotNull NpcSkinProvider skinProvider) {
        this.skinProvider = skinProvider;
        return this;
    }

    public NpcBuilder withMetadata(@NotNull String key, @NotNull Object value) {
        this.metadata.put(key, value);
        return this;
    }

    public Npc build(@NotNull NpcManager npcManager) {
        Npc npc = npcManager.createNpc(location);
        if (interactListener != null) {
            npc.setInteractListener(interactListener);
        }
        if (skinProvider != null) {
            npc.getVisibilityHandler().setSkinProvider(skinProvider);
        }
        if (renderFilter != null) {
            npc.getVisibilityHandler().setRenderFilter(renderFilter);
        }

        metadata.forEach(npc::setMetadata);
        return npc;
    }
}
