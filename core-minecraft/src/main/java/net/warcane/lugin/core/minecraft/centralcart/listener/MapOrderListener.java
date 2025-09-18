package net.warcane.lugin.core.minecraft.centralcart.listener;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import net.warcane.lugin.core.minecraft.centralcart.OrderActivatedEvent;
import net.warcane.lugin.core.minecraft.centralcart.models.Order;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
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
        
        Arrays.stream(player.getInventory().getContents()).filter(Objects::nonNull).filter(itemStack -> itemStack.getType() == Material.FILLED_MAP).forEach(itemStack -> {
            ReadableNBT read = NBT.readNbt(itemStack);
            if (!read.hasTag("isBuyMap") || !read.getString("orderId").equalsIgnoreCase(order.internalId())) {
                return;
            }
            
            player.getInventory().remove(itemStack);
            player.sendMessage(Component.textOfChildren(
                Component.newline(),
                Component.text("SUCESSO!", NamedTextColor.GREEN).appendNewline(),
                Component.text("A sua compra foi processada com sucesso. ", NamedTextColor.YELLOW)
                    .append(Component.text("(Order: %s)".formatted(order.internalId()), NamedTextColor.GRAY)),
                Component.text("Relogue no servidor para receber seus produtos.", NamedTextColor.YELLOW),
                Component.newline()
            ));
        });
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
			
			String orderId = read.getString("orderId");
            player.sendMessage(Component.textOfChildren(
                Component.newline(),
                Component.text("&cERROR!"),
                Component.newline(),
                Component.text("&cA compra com o ID &e%s &cfoi removida do seu inventário.\n".formatted(orderId)),
                Component.text("&cVocê ainda pode realizar o pagamento caso tenha ele salvo."),
                Component.text("Se precisar poderá gerar um novo código realizando a compra novamente."),
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
		String orderId = read.getString("orderId");
        player.sendMessage(Component.textOfChildren(
            Component.newline(),
            Component.text("ERROR!", NamedTextColor.RED),
            Component.newline(),
            Component.text("&cA compra com o ID &e%s &cfoi removida do seu inventário.\n".formatted(orderId)),
            Component.text("&cVocê ainda pode realizar o pagamento caso tenha ele salvo."),
            Component.text("Se precisar poderá gerar um novo código realizando a compra novamente."),
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
		String orderId = read.getString("orderId");
        player.sendMessage(Component.textOfChildren(
            Component.newline(),
            Component.text("&cERROR!"),
            Component.newline(),
            Component.text("&cA compra com o ID &e%s &cfoi removida do seu inventário.\n".formatted(orderId)),
            Component.text("&cVocê ainda pode realizar o pagamento caso tenha ele salvo."),
            Component.text("Se precisar poderá gerar um novo código realizando a compra novamente."),
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
		String orderId = read.getString("orderId");
        player.sendMessage(Component.textOfChildren(
            Component.newline(),
            Component.text("&cERROR!"),
            Component.newline(),
            Component.text("&cA compra com o ID &e%s &cfoi removida do seu inventário.\n".formatted(orderId)),
            Component.text("&cVocê ainda pode realizar o pagamento caso tenha ele salvo."),
            Component.text("Se precisar poderá gerar um novo código realizando a compra novamente."),
            Component.newline()
        ));
	}
	
	@EventHandler
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
	
}
