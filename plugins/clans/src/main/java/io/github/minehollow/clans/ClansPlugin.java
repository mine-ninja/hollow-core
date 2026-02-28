package io.github.minehollow.clans;

import io.github.minehollow.clans.command.ClanCommand;
import io.github.minehollow.clans.config.MessageConfig;
import io.github.minehollow.clans.hook.PapiHook;
import io.github.minehollow.clans.listener.ClanListener;
import io.github.minehollow.clans.menu.ClanConfirmationMenu;
import io.github.minehollow.clans.menu.ClanMainMenu;
import io.github.minehollow.clans.menu.ClanMembersMenu;
import io.github.minehollow.clans.menu.ClanPermissionMenu;
import io.github.minehollow.clans.menu.ClanSettingsMenu;
import io.github.minehollow.clans.menu.ClanTransferSelectMenu;
import io.github.minehollow.clans.service.ClanService;
import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.minecraft.plugin.SimplePlugin;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.configuration.ConfigurationSection;

@Slf4j
@Getter
public class ClansPlugin extends SimplePlugin {

    private ClanService clanService;
    private MessageConfig messageConfig;

    /** Flat array: slotTable[tier-1] = maxMembers for that tier */
    private int[] slotTable;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messageConfig = new MessageConfig(this);
        this.clanService = new ClanService();
        this.slotTable = loadSlotTable();

        registerCommands("clans", new ClanCommand(this));
        registerListeners(new ClanListener(clanService));

        // Register menus
        MenuUtil.registerMenus(
            new ClanMainMenu(this),
            new ClanMembersMenu(this),
            new ClanPermissionMenu(this),
            new ClanSettingsMenu(this),
            new ClanConfirmationMenu(this),
            new ClanTransferSelectMenu(this)
        );

        // Register PlaceholderAPI expansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PapiHook(clanService, slotTable).register();
            log.info("PlaceholderAPI detected — clan placeholders registered.");
        }

        log.info("ClansPlugin enabled — {} slot tiers loaded.", slotTable.length);
    }

    @Override
    public void onDisable() {
        if (clanService != null) {
            clanService.invalidateAll();
        }
    }

    /**
     * Reloads config.yml, messages.yml, and slot table.
     */
    public void reloadAll() {
        reloadConfig();
        messageConfig.reload();
        this.slotTable = loadSlotTable();
    }

    private int[] loadSlotTable() {
        ConfigurationSection section = getConfig().getConfigurationSection("upgrades.slots");
        if (section == null) return new int[]{10};

        var keys = section.getKeys(false).stream().sorted().toList();
        int[] table = new int[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            table[i] = section.getInt(keys.get(i) + ".max-members", 10);
        }
        return table;
    }
}

