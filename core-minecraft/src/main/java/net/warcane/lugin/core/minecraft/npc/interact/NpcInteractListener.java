package net.warcane.lugin.core.minecraft.npc.interact;

import net.warcane.lugin.core.minecraft.npc.Npc;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface NpcInteractListener {

    void handlePlayerInteract(@NotNull Npc npc, @NotNull Player player, @NotNull ClickType clickType);


    enum ClickType {
        LEFT_CLICK,
        RIGHT_CLICK;

        public boolean isLeftClick() {
            return this == LEFT_CLICK;
        }

        public boolean isRightClick() {
            return this == RIGHT_CLICK;
        }
    }
}
