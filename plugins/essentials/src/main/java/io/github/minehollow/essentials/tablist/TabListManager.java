package io.github.minehollow.essentials.tablist;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerListHeaderAndFooter;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.task.WrappedTask;
import io.github.minehollow.minecraft.util.MiniMessageColorExtractor;
import io.github.minehollow.minecraft.util.message.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the TabList header/footer, tab display names, and above-head nametags
 * using the Minikloon approach.
 * <p>
 * Optimized to avoid redundant packet sends, component rebuilds, and string
 * allocations on the hot path. Per-player state is cached and only rebuilt
 * when the resolved prefix/suffix actually changes.
 * <p>
 * Header and footer support multiple lines via YAML string lists in config.
 */
public class TabListManager {

    private static final long UPDATE_INTERVAL_TICKS = 5L;
    private static final TextColor WHITE = TextColor.color(0xFFFFFF);

    private final JavaPlugin plugin;
    private boolean enabled;

    /** Raw lines from config — joined with \n after placeholder resolution */
    private List<String> headerLines;
    private List<String> footerLines;

    private String prefixFormat;
    private String suffixFormat;
    private boolean belowNameEnabled;
    private String belowNameFormat;

    private @Nullable WrappedTask updateTask;

    // LuckPerms integration
    private boolean luckPermsAvailable;
    private @Nullable LuckPermsHook luckPermsHook;

    // PlaceholderAPI integration
    private boolean papiAvailable;

    // Sub-systems
    private NameTagManager nameTagManager;
    private BelowNameManager belowNameManager;
    private final TabListPacketListener tabListPacketListener;

    // Cached reference — avoid repeated API lookups
    private final PlayerManager packetPlayerManager;

    // Per-player cache to avoid rebuilding components when nothing changed
    private final Map<UUID, CachedPlayerState> playerCache = new ConcurrentHashMap<>();

    // Cached header/footer — only rebuilt when online/max changes
    private int lastOnline = -1;
    private int lastMax = -1;

    public TabListManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        detectLuckPerms();
        detectPlaceholderAPI();
        this.nameTagManager = new NameTagManager(luckPermsHook);
        this.tabListPacketListener = new TabListPacketListener();
        this.belowNameManager = new BelowNameManager(tabListPacketListener, belowNameFormat);
        this.packetPlayerManager = PacketEvents.getAPI().getPlayerManager();

        PacketEvents.getAPI().getEventManager().registerListener(tabListPacketListener);
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        this.enabled = config.getBoolean("tablist.enabled", true);

        // Suporta tanto string única quanto lista de strings no config
        this.headerLines = config.isList("tablist.header")
                           ? config.getStringList("tablist.header")
                           : List.of(config.getString("tablist.header", ""));

        this.footerLines = config.isList("tablist.footer")
                           ? config.getStringList("tablist.footer")
                           : List.of(config.getString("tablist.footer", ""));

        this.prefixFormat = config.getString("tablist.prefix", "");
        this.suffixFormat = config.getString("tablist.suffix", "");
        this.belowNameEnabled = config.getBoolean("tablist.belowname.enabled", true);
        this.belowNameFormat = config.getString("tablist.belowname.format",
            "<#FF5555>❤ {health} <#555555>│ <#FFAA00>Lv.{level}");
    }

    private void detectLuckPerms() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                this.luckPermsHook = new LuckPermsHook();
                this.luckPermsAvailable = true;
                plugin.getLogger().info("LuckPerms detected — TabList will use rank prefixes/suffixes.");
            } catch (NoClassDefFoundError | Exception ignored) {
                this.luckPermsAvailable = false;
            }
        }
    }

    private void detectPlaceholderAPI() {
        this.papiAvailable = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (papiAvailable) {
            plugin.getLogger().info("PlaceholderAPI detected — TabList will resolve PAPI placeholders.");
        }
    }

    public void start() {
        if (!enabled || updateTask != null) return;
        updateTask = Tasks.runAsyncRepeating(this::updateAll, 20L, UPDATE_INTERVAL_TICKS);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public void reload() {
        stop();
        loadConfig();
        playerCache.clear();
        lastOnline = -1;
        this.nameTagManager = new NameTagManager(luckPermsHook);
        this.belowNameManager = new BelowNameManager(tabListPacketListener, belowNameFormat);
        start();
    }

    // ── Per-player cached state ──────────────────────────────

    private static final class CachedPlayerState {
        String rawPrefix;
        String rawSuffix;
        Component teamPrefix;      // prefix + colored name (used for both tab and nametag)
        Component teamSuffix;      // suffix component
        Component tabDisplayName;  // prefix + colored name + suffix
        String dirtyKey;           // rawPrefix + rawSuffix for nametag dirty check

        int lastPing;
        String lastWorld;
    }

    // ── Tick loop ────────────────────────────────────────────

    private void updateAll() {
        var onlinePlayers = Bukkit.getOnlinePlayers();
        int online = onlinePlayers.size();
        int max = Bukkit.getMaxPlayers();

        boolean anyDisplayNameChanged = false;

        for (Player player : onlinePlayers) {
            if (!player.isOnline()) continue;

            UUID uuid = player.getUniqueId();
            int ping = player.getPing();
            String worldName = player.getWorld().getName();

            // ── Header / Footer ──
            sendHeaderFooterIfNeeded(player, online, max, ping, worldName, uuid);

            // ── Below-name display ──
            if (belowNameEnabled) {
                belowNameManager.update(player, this::applyBelowNamePlaceholders);
            }

            // ── Resolve prefix / suffix (used for both tab and nametag) ──
            String rawPrefix = resolvePrefix(player, online, max, ping, worldName);
            String rawSuffix = resolveSuffix(player, online, max, ping, worldName);

            // ── Dirty check ──
            CachedPlayerState cached = playerCache.get(uuid);
            if (cached != null && rawPrefix.equals(cached.rawPrefix) && rawSuffix.equals(cached.rawSuffix)) {
                continue;
            }

            // ── Rebuild components ──
            Component prefixComponent = StringUtils.formatString(rawPrefix);
            Component suffixComponent = StringUtils.formatString(rawSuffix);
            TextColor nameColor = resolveNameColor(rawPrefix);

            Component teamPrefix = Component.empty()
                .append(prefixComponent)
                .append(Component.text(player.getName()).color(nameColor));

            Component tabDisplayName = Component.empty()
                .append(teamPrefix)
                .append(suffixComponent);

            String dirtyKey = rawPrefix + "\1" + rawSuffix;

            if (cached == null) {
                cached = new CachedPlayerState();
                playerCache.put(uuid, cached);
            }
            cached.rawPrefix = rawPrefix;
            cached.rawSuffix = rawSuffix;
            cached.teamPrefix = teamPrefix;
            cached.teamSuffix = suffixComponent;
            cached.tabDisplayName = tabDisplayName;
            cached.dirtyKey = dirtyKey;

            // ── Update tab list display name ──
            tabListPacketListener.updateDisplayName(uuid, tabDisplayName);
            anyDisplayNameChanged = true;

            // ── Update nametag (same prefix/suffix as tab) ──
            nameTagManager.update(player, teamPrefix, suffixComponent, dirtyKey);
        }

        if (anyDisplayNameChanged) {
            tabListPacketListener.sendBatchDisplayNames(onlinePlayers);
        }

        lastOnline = online;
        lastMax = max;
    }

    private void sendHeaderFooterIfNeeded(@NotNull Player player, int online, int max,
                                          int ping, @NotNull String worldName, @NotNull UUID uuid) {
        CachedPlayerState cached = playerCache.get(uuid);

        boolean globalChanged = (online != lastOnline || max != lastMax);
        boolean perPlayerChanged = cached == null
                                   || cached.lastPing != ping
                                   || !worldName.equals(cached.lastWorld);

        if (!globalChanged && !perPlayerChanged) return;

        Component header = resolveMultilineComponent(headerLines, player, online, max, ping, worldName);
        Component footer = resolveMultilineComponent(footerLines, player, online, max, ping, worldName);

        packetPlayerManager.sendPacket(player,
            new WrapperPlayServerPlayerListHeaderAndFooter(header, footer));

        if (cached == null) {
            cached = new CachedPlayerState();
            playerCache.put(uuid, cached);
        }
        cached.lastPing = ping;
        cached.lastWorld = worldName;
    }

    // ── Join / Quit ──────────────────────────────────────────

    public void onPreJoin(@NotNull Player player) {
        if (!enabled) return;

        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        int ping = player.getPing();
        String worldName = player.getWorld().getName();

        String rawPrefix = resolvePrefix(player, online, max, ping, worldName);
        String rawSuffix = resolveSuffix(player, online, max, ping, worldName);

        Component prefixComponent = StringUtils.formatString(rawPrefix);
        Component suffixComponent = StringUtils.formatString(rawSuffix);
        TextColor nameColor = resolveNameColor(rawPrefix);

        Component teamPrefix = Component.empty()
            .append(prefixComponent)
            .append(Component.text(player.getName()).color(nameColor));

        Component tabDisplayName = Component.empty()
            .append(teamPrefix)
            .append(suffixComponent);

        tabListPacketListener.register(player.getUniqueId(), tabDisplayName);

        CachedPlayerState cached = new CachedPlayerState();
        cached.rawPrefix = rawPrefix;
        cached.rawSuffix = rawSuffix;
        cached.teamPrefix = teamPrefix;
        cached.teamSuffix = suffixComponent;
        cached.tabDisplayName = tabDisplayName;
        cached.dirtyKey = rawPrefix + "\1" + rawSuffix;
        cached.lastPing = ping;
        cached.lastWorld = worldName;
        playerCache.put(player.getUniqueId(), cached);
    }

    public void onJoin(@NotNull Player player) {
        if (!enabled) return;
        Tasks.runAsyncLater(() -> {
            if (!player.isOnline()) return;

            UUID uuid = player.getUniqueId();
            CachedPlayerState cached = playerCache.get(uuid);
            if (cached == null) return;

            String tablistUsername = tabListPacketListener.getTablistUsername(uuid);
            if (tablistUsername == null) return;

            nameTagManager.onJoin(player, tablistUsername,
                cached.teamPrefix, cached.teamSuffix, cached.dirtyKey);

            tabListPacketListener.sendAllDisplayNamesTo(player);
            tabListPacketListener.sendDisplayNameToAll(uuid);

            int online = Bukkit.getOnlinePlayers().size();
            int max = Bukkit.getMaxPlayers();
            sendHeaderFooterIfNeeded(player, online, max, player.getPing(),
                player.getWorld().getName(), uuid);

            if (belowNameEnabled) {
                belowNameManager.onJoin(player, Bukkit.getOnlinePlayers(), this::applyBelowNamePlaceholders);
            }
        }, 5L);
    }

    public void onQuit(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        playerCache.remove(uuid);
        belowNameManager.onQuit(player);
        tabListPacketListener.unregister(uuid);
        nameTagManager.onQuit(player);
        lastOnline = -1;
    }

    // ── Multiline resolution ─────────────────────────────────

    /**
     * Resolve placeholders em cada linha da lista, formata via MiniMessage
     * e une os componentes com {@link Component#newline()}.
     * <p>
     * Cada linha é parseada individualmente, preservando cores, gradientes
     * e qualquer outra tag MiniMessage por linha.
     */
    private @NotNull Component resolveMultilineComponent(
        @NotNull List<String> lines,
        @NotNull Player player,
        int online, int max, int ping,
        @NotNull String worldName
    ) {
        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            String resolved = applyPlaceholders(lines.get(i), player, online, max, ping, worldName);
            result = result.append(StringUtils.formatString(resolved));
            if (i < lines.size() - 1) {
                result = result.append(Component.newline());
            }
        }
        return result;
    }

    // ── Color resolution ─────────────────────────────────────

    private @NotNull TextColor resolveNameColor(@NotNull String rawPrefix) {
        String colorTag = MiniMessageColorExtractor.extractLastColorTag(rawPrefix);
        if (colorTag == null) return WHITE;
        TextColor parsed = parseColorTag(colorTag);
        return parsed != null ? parsed : WHITE;
    }

    private static @Nullable TextColor parseColorTag(@NotNull String tag) {
        if (tag.startsWith("<#") && tag.endsWith(">")) {
            return TextColor.fromHexString(tag.substring(1, tag.length() - 1));
        }
        if (tag.startsWith("<gradient:")) {
            int end = tag.length() - 1;
            int firstColon = tag.indexOf(':', 10);
            if (firstColon > 0 && firstColon < end) {
                return TextColor.fromHexString(tag.substring(10, firstColon));
            }
        }
        return null;
    }

    // ── Prefix / suffix resolution ───────────────────────────

    private @NotNull String resolvePrefix(@NotNull Player player, int online, int max, int ping, @NotNull String world) {
        if (luckPermsAvailable && luckPermsHook != null) {
            String lpPrefix = luckPermsHook.getPrefix(player);
            if (lpPrefix != null && !lpPrefix.isEmpty()) {
                return applyPlaceholders(lpPrefix, player, online, max, ping, world);
            }
        }
        return applyPlaceholders(prefixFormat, player, online, max, ping, world);
    }

    private @NotNull String resolveSuffix(@NotNull Player player, int online, int max, int ping, @NotNull String world) {
        if (luckPermsAvailable && luckPermsHook != null) {
            String lpSuffix = luckPermsHook.getSuffix(player);
            if (lpSuffix != null && !lpSuffix.isEmpty()) {
                return applyPlaceholders(lpSuffix, player, online, max, ping, world);
            }
        }
        return applyPlaceholders(suffixFormat, player, online, max, ping, world);
    }


    private @NotNull String applyPlaceholders(@NotNull String text, @NotNull Player player,
                                              int online, int max, int ping, @NotNull String world) {
        String result = text
            .replace("{player}", player.getName())
            .replace("{online}", String.valueOf(online))
            .replace("{max}", String.valueOf(max))
            .replace("{ping}", String.valueOf(ping))
            .replace("{world}", world)
            .replace("{health}", String.valueOf((int) Math.ceil(player.getHealth())))
            .replace("{max_health}", String.valueOf((int) Math.ceil(player.getMaxHealth())))
            .replace("{level}", String.valueOf(player.getLevel()));

        if (papiAvailable) {
            result = PapiHook.setPlaceholders(player, result);
        }
        return result;
    }

    private @NotNull String applyBelowNamePlaceholders(@NotNull String format, @NotNull Player player) {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        return applyPlaceholders(format, player, online, max, player.getPing(), player.getWorld().getName());
    }

    private static final class PapiHook {
        static @NotNull String setPlaceholders(@NotNull Player player, @NotNull String text) {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        }
    }
}