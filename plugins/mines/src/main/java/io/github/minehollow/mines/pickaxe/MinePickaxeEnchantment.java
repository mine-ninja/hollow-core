package io.github.minehollow.mines.pickaxe;

import io.github.minehollow.mines.event.VirtualMineBlockBreakEvent;
import io.github.minehollow.mines.instance.MineInstance;
import org.jetbrains.annotations.NotNull;

public interface MinePickaxeEnchantment {

    void handleBlockBreak(@NotNull VirtualMineBlockBreakEvent event, @NotNull MineInstance mineInstance);


}
