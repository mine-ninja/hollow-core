package io.github.minehollow.minecraft.centralcart.listener;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import io.github.minehollow.minecraft.BukkitPlatformPlugin;
import io.github.minehollow.minecraft.centralcart.events.OrderExpiredEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class MapWatcher implements Runnable {
    @Override
    public void run() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player player : players) {
            if (!player.getInventory().contains(Material.FILLED_MAP)) continue;
            
            for (ItemStack stack : player.getInventory().getStorageContents()) {
                if (stack == null) continue;
                
                ReadableNBT read = NBT.readNbt(stack);
                if (!read.hasTag("isBuyMap") || !read.hasTag("orderId") || !read.hasTag("timeMap")) continue;
                
                long diff = read.getLong("timeMap") - System.currentTimeMillis();
                int seconds = (int) (diff / 1000L);
                if (seconds > 0) continue;
                
                player.getInventory().remove(stack);
                Bukkit.getPluginManager().callEvent(new OrderExpiredEvent(player.getName(), read.getString("orderId")));
                
                BukkitPlatformPlugin.getInstance().adventure().player(player)
                    .sendMessage(Component.textOfChildren(
                        Component.newline(),
                        Component.text("ERROR!", NamedTextColor.RED, TextDecoration.BOLD).appendNewline(),
                        Component.newline(),
                        Component.text("A compra foi removida do seu inventário.", NamedTextColor.RED).appendNewline(),
                        Component.text("Você ainda pode realizar o pagamento caso tenha ele salvo.", NamedTextColor.RED).appendNewline(),
                        Component.text("Você pode gerar um novo código para comprar novamente.", NamedTextColor.RED).appendNewline(),
                        Component.newline()
                    ));
            }
        }
    }
}
