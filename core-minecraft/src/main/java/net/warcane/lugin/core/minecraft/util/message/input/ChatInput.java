package net.warcane.lugin.core.minecraft.util.message.input;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author Rok, Pedro Lucas nmm. 04/01/2026
 * @project lugin-core
 */
public class ChatInput {

    private static final Map<UUID, InputSession> inputs = new ConcurrentHashMap<>();

    public static void init(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new ChatInputEvents(), plugin);
        startScheduler();
    }

    private static void startScheduler() {
        Tasks.runAsyncRepeating(() -> {
            inputs.forEach((uuid, inputSession) -> {
                if (inputSession.isExpired()) {
                    Player player = BukkitPlatformPlugin.getInstance().getServer().getPlayer(uuid);
                    if (player != null) {
                        inputSession.whenExpire().accept(player);
                    }
                    inputs.remove(uuid);
                }
            });
        }, 20, 20);
    }

    public static void waitInput(UUID playerId, Consumer<String> inputHandler) {
        inputs.put(playerId, InputSession.of(playerId, inputHandler));
    }

    public static void waitInput(Player player, Consumer<String> inputHandler) {
        waitInput(player.getUniqueId(), inputHandler);
    }

    public static void waitInput(Player player, Consumer<String> inputHandler, String messageComponent) {
        final var audience = BukkitPlatformPlugin.getInstance().adventure().player(player);
        StringUtils.send(audience, messageComponent);
        waitInput(player, inputHandler);
    }

    public static void waitInput(Player player, Consumer<String> inputHandler, Long expireInMillis, String messageComponent) {
        final var audience = BukkitPlatformPlugin.getInstance().adventure().player(player);
        StringUtils.send(audience, messageComponent);
        inputs.put(player.getUniqueId(), InputSession.of(player.getUniqueId(), inputHandler, System.currentTimeMillis() + expireInMillis));
    }

    public static void waitInput(Player player, Consumer<String> inputHandler, Long expireInMillis) {
        waitInput(player, inputHandler, expireInMillis, "<l-info>Por favor, insira sua resposta no chat. Você tem " + (expireInMillis / 1000) + " segundos.");
    }

    public static void waitInput(Player player, InputSession inputSession) {
        inputs.put(player.getUniqueId(), inputSession);
    }

    public static InputSession remove(UUID playerId) {
        return inputs.remove(playerId);
    }

    public static boolean contains(Player player) {
        return contains(player.getUniqueId());
    }

    public static boolean contains(UUID playerId) {
        return inputs.containsKey(playerId);
    }

    protected static void playerReply(Player player, String message) {
        InputSession inputSession = remove(player.getUniqueId());
        if (inputSession != null) {
            inputSession.inputSupplier().accept(message);
        }
    }
}
