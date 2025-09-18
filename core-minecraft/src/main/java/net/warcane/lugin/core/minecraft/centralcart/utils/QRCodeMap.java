package net.warcane.lugin.core.minecraft.centralcart.utils;

import de.tr7zw.changeme.nbtapi.NBT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class QRCodeMap {
	public static void generateSupportedMap(final BufferedImage image, final Player player, final String displayName, final List<String> lore, final String orderId) {
		try {
			ItemStack stack = ItemStack.of(Material.FILLED_MAP);
			stack.editMeta(meta -> {
                meta.displayName(Component.text(displayName).decoration(TextDecoration.ITALIC, false));
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.values());
                
				if (meta instanceof MapMeta mapMeta) {
					final MapView mapView = Bukkit.createMap(player.getWorld());
					mapView.getRenderers().clear();
					mapView.setScale(MapView.Scale.FARTHEST);
					
					mapView.addRenderer(new QRCodeMapRenderer(image));
					mapMeta.setMapView(mapView);
				}
			});
			
			NBT.modify(stack, nbt -> {
				nbt.setBoolean("isBuyMap", Boolean.TRUE);
				nbt.setString("orderId", orderId);
				nbt.setLong("timeMap", System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3L));
			});
			player.getInventory().addItem(stack);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static BufferedImage decodeQRCodeToImage(final String base64QRCode) throws IOException {
		final byte[] imageBytes = Base64.getDecoder().decode(base64QRCode);
		try (final ByteArrayInputStream b = new ByteArrayInputStream(imageBytes)) {
			return ImageIO.read(b);
		}
	}
	
	private static class QRCodeMapRenderer extends MapRenderer {
		private final BufferedImage image;
		private boolean rendered;
		
		public QRCodeMapRenderer(final BufferedImage image) {
			this.rendered = false;
			this.image = image;
		}
		
		public void render(final MapView mapView, final MapCanvas mapCanvas, final Player player) {
			if (this.rendered) {
				return;
			}
			this.rendered = true;
			final BufferedImage resizedImage = new BufferedImage(128, 128, 2);
			resizedImage.getGraphics().drawImage(this.image, 0, 0, 128, 128, null);
			mapCanvas.drawImage(0, 0, resizedImage);
		}
	}
}
