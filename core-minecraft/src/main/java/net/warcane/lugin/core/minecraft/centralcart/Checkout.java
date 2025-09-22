package net.warcane.lugin.core.minecraft.centralcart;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.centralcart.models.Coupon;
import net.warcane.lugin.core.minecraft.centralcart.models.Product;
import net.warcane.lugin.core.minecraft.centralcart.utils.QRCodeMap;
import net.warcane.lugin.core.minecraft.centralcart.utils.Response;
import net.warcane.lugin.core.minecraft.task.Tasks;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

public class Checkout {
	public void createCheckout(final Product product, final Coupon coupon, final Player player, Consumer<CheckoutResult> onResult) {
		ItemStack hand = player.getInventory().getItemInMainHand();
		if (hand.getType() != Material.AIR) {
            onResult.accept(new CheckoutResult(player, false, Map.of("status", "item_in_hand")));
			return;
		}
		
		final JsonObject checkout = new JsonObject();
		checkout.addProperty("gateway", "PIX");
		checkout.addProperty("client_email", player.getName() + "@autopix.com");
		checkout.addProperty("client_name", "Auto PIX");
		checkout.addProperty("client_discord", player.getName() + "-AutoPix");
		checkout.addProperty("terms", Boolean.TRUE);
		
		final JsonObject variables = new JsonObject();
		variables.addProperty("client_identifier", player.getName());
		checkout.add("variables", variables);
		
		final JsonArray jsonArrayCart = new JsonArray();
		final JsonObject cartItem = new JsonObject();
		cartItem.addProperty("package_id", product.id());
		cartItem.addProperty("quantity", 1);
		jsonArrayCart.add(cartItem);
		checkout.add("cart", jsonArrayCart);
		checkout.addProperty("coupon", (coupon == null) ? "" : coupon.coupon());
		
		Tasks.runAsync(() -> {
			CentralCart centralCart = BukkitPlatform.getInstance().getCentralCart();
			
			final Response response = centralCart.perform("https://api.centralcart.com.br/v1/app/checkout", "POST", checkout);
			if (!response.isSuccessful()) {
                onResult.accept(new CheckoutResult(player, false, Map.of("status", "response_not_successful")));
                centralCart.getLogger().error("An error occurred while trying to generate the checkout for the player {}, the response json is: {}", player.getName(), response.body());
				return;
			}
			
			try {
				JsonObject jsonObject = this.parseJson(response.body());
				if (jsonObject == null) {
                    onResult.accept(new CheckoutResult(player, false));
					return;
				}
				
				String return_url = this.getStringField(jsonObject, "return_url");
				String qrCode = this.getStringField(jsonObject, "qr_code");
				
				Tasks.runSync(() -> {
					BufferedImage imgQrcode2;
					try {
						imgQrcode2 = QRCodeMap.decodeQRCodeToImage(qrCode);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					QRCodeMap.generateSupportedMap(imgQrcode2, player, "§aCódigo QrCode", Collections.singletonList("§7Compra de " + player.getName()), jsonObject.get("order_id").getAsString());
				});
				
                onResult.accept(new CheckoutResult(player, true, Map.of("return_url", return_url)));
			} catch (Exception exception) {
                onResult.accept(new CheckoutResult(player, false, Map.of("status", "exception")));
                centralCart.getLogger().error("An error occurred while trying to generate the checkout for the player {}, the response json is: {}", player.getName(), response.body());
				exception.printStackTrace();
			}
		});
	}
	
	public JsonObject parseJson(final String responseBody) {
		final JsonElement jsonElement = JsonParser.parseString(responseBody);
		return jsonElement.isJsonObject() ? jsonElement.getAsJsonObject() : null;
	}
	
	public String getStringField(final JsonObject jsonObject, final String fieldName) {
		return (jsonObject.has(fieldName) && jsonObject.get(fieldName).isJsonPrimitive()) ? jsonObject.get(fieldName).getAsString() : null;
	}
    
    public record CheckoutResult(Player player, boolean isSuccessful, Map<String, Object> data) {
        public CheckoutResult(Player player, boolean isSuccessful) {
            this(player, isSuccessful, Map.of());
        }
    }
}
