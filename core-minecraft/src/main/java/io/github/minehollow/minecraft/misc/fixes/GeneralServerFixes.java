package io.github.minehollow.minecraft.misc.fixes;

import io.github.minehollow.minecraft.util.message.StringUtils;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap; // Nativo no Paper, muito mais rápido que HashMap
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class GeneralServerFixes implements Listener {

    // Itens que não devem existir como bloco ou item dropado (Segurança)
    private static final Set<Material> BANNED_ITEMS = Set.of(
        Material.VAULT,
        Material.TRIAL_SPAWNER,
        Material.COMMAND_BLOCK,
        Material.STRUCTURE_BLOCK,
        Material.JIGSAW
    );

    // Uso de FastUtil para performance extrema e menos GC (Garbage Collector)
    private final Object2LongOpenHashMap<UUID> joinCommandCooldown = new Object2LongOpenHashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (BANNED_ITEMS.contains(event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (BANNED_ITEMS.contains(event.getEntity().getItemStack().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupItem(@NotNull PlayerAttemptPickupItemEvent event) {
        if (BANNED_ITEMS.contains(event.getItem().getItemStack().getType())) {
            event.setCancelled(true);
            event.getItem().remove();
            event.getPlayer().sendMessage(StringUtils.formatString("§cEste item é proibido e foi removido."));
        }
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        // Cooldown de 5 segundos após logar para evitar spam de bots/comandos
        joinCommandCooldown.put(event.getPlayer().getUniqueId(), System.currentTimeMillis() + 5000);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Importante: Limpar o mapa para evitar Memory Leak
        joinCommandCooldown.removeLong(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void handleCommandCooldown(@NotNull PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("lugin.staff")) return;

        // Bloqueia comandos se o player estiver morto (exploit comum)
        if (player.isDead()) {
            event.setCancelled(true);
            return;
        }

        long cooldown = joinCommandCooldown.getLong(player.getUniqueId());
        if (System.currentTimeMillis() < cooldown) {
            player.sendMessage(StringUtils.formatString("§cAguarde para usar comandos após entrar."));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerClickInventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Verificação de Dessincronização (Anti-Ghost Inventory Dupe)
        if (player.getOpenInventory().getTopInventory() != event.getInventory()) {
            event.setCancelled(true);
            player.closeInventory();
            Bukkit.getConsoleSender().sendMessage("§c[Security] " + player.getName() + " tentou interagir com inventário desincronizado.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageClose(EntityDamageEvent event) {
        // Fechar inventário ao levar dano previne dupes baseados em timing de fechar menu
        if (event.getEntity() instanceof Player player) {
            InventoryType type = player.getOpenInventory().getTopInventory().getType();
            if (type != InventoryType.CRAFTING && type != InventoryType.CREATIVE) {
                player.closeInventory();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEdit(PlayerEditBookEvent event) {
        // Prevenção de crash por caracteres ilegais ou sobrecarga de NBT
        for (String page : event.getNewBookMeta().getPages()) {
            if (!StandardCharsets.ISO_8859_1.newEncoder().canEncode(page) || page.contains("\u0000")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(StringUtils.formatString("§cCaracteres inválidos detectados no livro."));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void preventInteractionsWithOpenInv(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        // Impede comandos (exceto chat se necessário) com inventários abertos
        if (player.getOpenInventory().getTopInventory().getType() != InventoryType.CRAFTING) {
            player.sendMessage(StringUtils.formatString("§cFeche o inventário antes de usar comandos."));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void preventBreakingWithOpenInv(BlockBreakEvent event) {
        Player player = event.getPlayer();
        // Impede quebrar blocos enquanto o inventário de um container está aberto (Anti-Packets)
        InventoryType type = player.getOpenInventory().getTopInventory().getType();
        if (type != InventoryType.CRAFTING && type != InventoryType.CREATIVE) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage(StringUtils.formatString("§cVocê não pode quebrar blocos com um inventário aberto."));
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // Otimização: Só checa se o jogador mudou de bloco real (não apenas girou a cabeça)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        InventoryType type = player.getOpenInventory().getTopInventory().getType();

        // Se o player se afastar muito de um container aberto, fecha o inventário (Anti-Reach)
        if (type != InventoryType.CRAFTING && type != InventoryType.CREATIVE && type != InventoryType.PLAYER) {
            InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof org.bukkit.block.BlockState blockState) {
                if (player.getLocation().distanceSquared(blockState.getLocation()) > 36) { // 6 blocos de distância
                    player.closeInventory();
                }
            }
        }
    }
}