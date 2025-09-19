package net.warcane.lugin.core.minecraft.menu.config;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.warcane.lugin.core.minecraft.menu.MenuLayout;
import net.warcane.lugin.core.minecraft.util.sound.PredefinedSound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuConfig {

    @Builder.Default
    private Object title = "";
    @Builder.Default
    private int rows = 1;
    @Builder.Default
    private boolean globalClickCancelled = true;
    @Builder.Default
    private boolean globalDragCancelled = true;
    @Builder.Default
    private boolean tickUpdateEnabled = false;

    @Builder.Default
    private long updateIntervalMillis = 1;
    
    private MenuLayout layout;

    @Builder.Default
    private PredefinedSound clickSound = null;
    @Builder.Default
    private PredefinedSound closeSound = null;

    public MenuConfig(@NotNull MenuConfig config) {
        this.title = config.title;
        this.rows = config.rows;
        if (config.getLayout() != null) {
            this.setLayout(config.getLayout());
        }
        this.globalClickCancelled = config.globalClickCancelled;
        this.globalDragCancelled = config.globalDragCancelled;
        this.tickUpdateEnabled = config.tickUpdateEnabled;
        this.updateIntervalMillis = config.updateIntervalMillis;
        this.clickSound = config.clickSound;
        this.closeSound = config.closeSound;
    }

    public void setOptions(@NotNull MenuConfig options) {
        setTitle(options.getTitle());
        setRows(options.getRows());
        setLayout(options.getLayout());
        setGlobalClickCancelled(options.isGlobalClickCancelled());
        setGlobalDragCancelled(options.isGlobalDragCancelled());
        setTickUpdateEnabled(options.isTickUpdateEnabled());
        setUpdateIntervalMillis(options.getUpdateIntervalMillis());
        setClickSound(options.getClickSound());
        setCloseSound(options.getCloseSound());
    }
    
    public void setLayout(String... lines) {
        this.setLayout(MenuLayout.of(lines));
    }
    
    public void setLayout(@NotNull MenuLayout layout) {
        this.layout = layout;
        setRows(layout.rows());
    }

    public void setUpdateIntervalMillis(long intervalMillis, TimeUnit unit) {
        if (intervalMillis < 1) {
            intervalMillis = 1;
        }
        this.updateIntervalMillis = unit.toMillis(intervalMillis);
    }

    public void setTitle(@NotNull String title) {
        this.title = title;
    }

    public void setTitle(@NotNull Component component) {
        this.title = component;
    }

    public void setTitle(Object defaultTitle) {
        switch (defaultTitle) {
            case String str -> setTitle(str);
            case Component comp -> setTitle(comp);
            default ->
              throw new IllegalArgumentException("Unsupported title type: " + defaultTitle.getClass().getName());
        }
    }

    public void playClickSound(@NotNull Player player) {
        if (clickSound != null) {
            clickSound.play(player);
        }
    }

    public void playCloseSound(@NotNull Player player) {
        if (closeSound != null) {
            closeSound.play(player);
        }
    }
}
