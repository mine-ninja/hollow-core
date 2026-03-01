package io.github.minehollow.zones;

import io.github.minehollow.minecraft.plugin.SimplePlugin;
import io.github.minehollow.zones.command.ZonesCommand;
import io.github.minehollow.zones.listener.*;
import io.github.minehollow.zones.placeholder.ZonesPlaceholderExpansion;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ZonesPlugin extends SimplePlugin {

    private ZoneManager zoneManager;
    private ZoneQuery zoneQuery;
    private ZoneMovementListener movementListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Core systems
        this.zoneManager = new ZoneManager(this);
        this.zoneQuery = new ZoneQuery(zoneManager);

        // Load zones from config
        zoneManager.loadAll();

        // Movement tracking (must be first, manages enter/exit events + visibility)
        this.movementListener = new ZoneMovementListener(zoneManager, zoneQuery);

        // Register all listeners
        registerListeners(
            movementListener,
            new BlockProtectionListener(zoneQuery),
            new EntityProtectionListener(zoneQuery),
            new PlayerProtectionListener(zoneQuery)
        );

        // Register command
        registerCommands("zones", new ZonesCommand(this));

        // PlaceholderAPI integration
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ZonesPlaceholderExpansion(zoneManager, zoneQuery).register();
            log.info("PlaceholderAPI expansion registered.");
        }

        log.info("Zones plugin enabled. {} zones loaded.", zoneManager.getZones().size());
    }

    @Override
    public void onDisable() {
        if (zoneManager != null) {
            zoneManager.saveAll();
        }
    }
}

