package io.github.minehollow.mines;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.plugin.SimplePlugin;
import io.github.minehollow.mines.command.MineCommand;
import io.github.minehollow.mines.config.Messages;
import io.github.minehollow.mines.instance.MineInstanceManager;
import io.github.minehollow.mines.listener.VirtualMineBlockProtectionListener;
import io.github.minehollow.mines.listener.VirtualMineListener;
import io.github.minehollow.mines.listener.VirtualMinePacketListener;
import io.github.minehollow.mines.menu.MineMainMenu;
import io.github.minehollow.mines.mine.MineDefinitionRegistry;
import io.github.minehollow.mines.mine.VirtualMineBlockResolver;
import io.github.minehollow.mines.render.VirtualMineRenderer;
import io.github.minehollow.mines.service.VirtualMineService;
import java.io.File;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class MinesPlugin extends SimplePlugin {

    private MineDefinitionRegistry definitionRegistry;
    private MineInstanceManager instanceManager;
    private VirtualMineRenderer renderer;
    private VirtualMineService virtualMineService;
    private VirtualMinePacketListener packetListener;

    private static Messages messages;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.saveResource("messages.yml", false);

        messages = new Messages(new File(this.getDataFolder(), "messages.yml"));

        this.definitionRegistry = new MineDefinitionRegistry();
        this.definitionRegistry.reload(this.getConfig());

        this.instanceManager = new MineInstanceManager();
        this.renderer = new VirtualMineRenderer(
            this.getConfig().getString("mine-world-name", "world"),
            new VirtualMineBlockResolver()
        );

        this.virtualMineService = new VirtualMineService(
            this.definitionRegistry,
            this.instanceManager,
            this.renderer
        );

        this.packetListener = new VirtualMinePacketListener(this, this.virtualMineService, this.renderer);

        BukkitPlatform.getInstance().getMenuManager().register(new MineMainMenu(this, this.virtualMineService));

        this.registerListeners(new VirtualMineListener(this, this.virtualMineService));
        this.registerListeners(new VirtualMineBlockProtectionListener(this.virtualMineService));
        PacketEvents.getAPI().getEventManager().registerListener(this.packetListener);
        this.registerCommands("mine", new MineCommand(this));

        log.info("Virtual mine system enabled with {} mine definitions.", this.definitionRegistry.getAll().size());
    }

    public static Messages messages() {
        return messages;
    }
}
