package net.warcane.lugin.core.minecraft.centralcart.listener;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import net.warcane.lugin.core.minecraft.centralcart.events.OrderActivatedEvent;
import net.warcane.lugin.core.minecraft.centralcart.models.Order;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class MapOrderListener implements Listener {
	private final BukkitPlatformPlugin plugin;
	
	public MapOrderListener(BukkitPlatformPlugin plugin) {
		this.plugin = plugin;
	}
    
    @EventHandler
    void onProductActive(OrderActivatedEvent event) {
        Player player = Bukkit.getPlayer(event.getUserId());
        if (player == null) return;
        
        Order order = event.getOrder();
        if (!player.getInventory().contains(Material.FILLED_MAP)) return;
        
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack == null || itemStack.getType() != Material.FILLED_MAP) continue;
            
            ReadableNBT read = NBT.readNbt(itemStack);
            if (!read.hasTag("isBuyMap") || !read.getString("orderId").equalsIgnoreCase(order.internalId())) continue;
            
            player.getInventory().remove(itemStack);
            BukkitAudiences adventure = BukkitPlatformPlugin.getInstance().adventure();
            adventure.player(player).sendMessage(Component.textOfChildren(
                Component.newline(),
                Component.text("SUCESSO!", NamedTextColor.GREEN, TextDecoration.BOLD).appendNewline(),
                Component.newline(),
                Component.textOfChildren(
                    Component.text("Compra processada com sucesso. ", NamedTextColor.GREEN),
                    Component.text("(ID: %s)".formatted(order.internalId()), NamedTextColor.GRAY)
                ).appendNewline(),
                Component.text("Relogue no servidor para receber seus produto(s).", NamedTextColor.GREEN).appendNewline(),
                Component.newline()
            ));
            adventure.player(player).playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, Sound.Source.PLAYER, 0.5F, 1.0F), Sound.Emitter.self());
        }
    }
    
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		
		if (event.getClick() == ClickType.NUMBER_KEY) {
			ItemStack stack = player.getInventory().getItem(event.getHotbarButton());
			if (stack == null || stack.getType() != Material.FILLED_MAP) return;
			
			ReadableNBT read = NBT.readNbt(stack);
			if (!read.hasTag("isBuyMap")) return;
			
			player.getInventory().remove(stack);
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
			event.setCancelled(true);
			return;
		}
		
		ItemStack currentItem = event.getCurrentItem();
		if (currentItem == null || currentItem.getType() != Material.FILLED_MAP) return;
		
		ReadableNBT read = NBT.readNbt(currentItem);
		if (!read.hasTag("isBuyMap")) return;
		
		player.getInventory().remove(currentItem);
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
		event.setCancelled(true);
	}
	
	@EventHandler
	void onInventoryMoveItem(InventoryMoveItemEvent event) {
		Optional<HumanEntity> optionalHumanEntity = event.getSource().getViewers().stream().findFirst();
		if (optionalHumanEntity.isEmpty()) return;
		
		Player player = (Player) optionalHumanEntity.get();
		ItemStack itemStack = event.getItem();
		if (itemStack == null || itemStack.getType() != Material.FILLED_MAP) return;
		
		ReadableNBT read = NBT.readNbt(itemStack);
		if (!read.hasTag("isBuyMap")) return;
		
		player.getInventory().remove(itemStack);
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
		event.setCancelled(true);
	}
	
	@EventHandler
	void onPlayerDropItem(PlayerDropItemEvent event) {
		Item item = event.getItemDrop();
		if (item == null) return;
		
		ItemStack itemStack = item.getItemStack();
		if (itemStack == null || itemStack.getType() != Material.FILLED_MAP) return;
		
		Player player = event.getPlayer();
		
		ReadableNBT read = NBT.readNbt(itemStack);
		if (!read.hasTag("isBuyMap")) return;
		
		item.remove();
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
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerItemFrameChange(PlayerItemFrameChangeEvent event) {
        ReadableNBT read = NBT.readNbt(event.getItemStack());
        if (read.hasTag("isBuyMap")) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
	void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Arrays.stream(player.getInventory().getContents())
			.filter(Objects::nonNull)
			.filter(itemStack -> itemStack.getType() == Material.FILLED_MAP)
			.forEach(itemStack -> {
				ReadableNBT read = NBT.readNbt(itemStack);
				if (!read.hasTag("isBuyMap")) return;
				
				player.getInventory().remove(itemStack);
			});
	}
 
	@EventHandler(priority = EventPriority.LOW)
	void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		Arrays.stream(player.getInventory().getContents())
			.filter(Objects::nonNull)
			.filter(itemStack -> itemStack.getType() == Material.FILLED_MAP)
			.forEach(itemStack -> {
				ReadableNBT read = NBT.readNbt(itemStack);
				if (!read.hasTag("isBuyMap")) return;
				
				player.getInventory().remove(itemStack);
			});
	}
	
}
