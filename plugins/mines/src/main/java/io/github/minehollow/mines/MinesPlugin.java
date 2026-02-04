package io.github.minehollow.mines;

import io.github.minehollow.minecraft.plugin.SimplePlugin;
import io.github.minehollow.mines.command.MinesCommand;
import io.github.minehollow.mines.listener.BlockBreakListener;
import io.github.minehollow.mines.listener.MineResetTaskListener;
import io.github.minehollow.mines.model.MineManager;
import lombok.Getter;

@Getter
public class MinesPlugin extends SimplePlugin {

    private MineManager mineManager;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        this.mineManager = new MineManager(this);
        this.mineManager.initializeMines();

        registerListeners(
          new BlockBreakListener(this),
          new MineResetTaskListener(this)
        );

        registerCommands("skills", new MinesCommand(this));
    }
}
