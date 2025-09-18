package net.warcane.lugin.core.minecraft.centralcart;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import java.util.function.Consumer;

public class Checkout {
	public void createCheckout(final Product product, final Coupon coupon, final Player player, Consumer<CheckoutResult> onSuccess, Consumer<CheckoutResult> onFailure) {
		ItemStack hand = player.getInventory().getItemInMainHand();
		if (hand.getType() != Material.AIR) {
            onFailure.accept(new CheckoutResult(CheckoutResult.Type.HAND_NOT_EMPTY, player));
			// player.sendMessage("&7[&c✖&7] &cNão é possível criar um checkout pois existe um item em sua mão.");
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
                onFailure.accept(new CheckoutResult(CheckoutResult.Type.CHECKOUT_CREATION_FAILED, player));
				// player.sendMessage("&7[&c✖&7] &cFalha ao tentar criar o checkout! Entre em contato com um administrador.");
                centralCart.getLogger().info("An error occurred while trying to generate the checkout for the player {}, the response json is: {}", player.getName(), response.body());
				return;
			}
			
			try {
				JsonObject jsonObject = this.parseJson(response.body());
				if (jsonObject == null) {
                    onFailure.accept(new CheckoutResult(CheckoutResult.Type.CHECKOUT_CREATION_FAILED, player));
					// player.sendMessage("&7[&c✖&7] &cFalha ao gerar o checkout!");
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
				
                onSuccess.accept(new CheckoutResult(CheckoutResult.Type.CHECKOUT_CREATION_SUCCESS, player));
				/*player.sendMessage(Component.textOfChildren(
                    Component.newline(),
                    Component.text("Checkout criado com sucesso!", NamedTextColor.GREEN),
                    Component.text("\nSiga as instruções abaixo para finalizar sua compra.", NamedTextColor.GREEN),
                    Component.newline(),
                    Component.text(" - Escaneie o QrCode que foi colocado em seu inventário.", NamedTextColor.YELLOW),
                    Component.newline(),
                    Component.newline(),
                    Component.text("&aClique ", NamedTextColor.GREEN),
                    Component.text("AQUI ", NamedTextColor.YELLOW).clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(return_url)),
                    Component.text("&apara fazer o pagamento diretamente pelo seu navegador.", NamedTextColor.GREEN)
                ));*/
			} catch (Exception exception) {
				player.sendMessage("&7[&c✖&7] &cFalha ao tentar criar o checkout! Entre em contato com um administrador.");
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
    
    public record CheckoutResult(Type type, Player player) {
        public enum Type {
            HAND_NOT_EMPTY,
            CHECKOUT_CREATION_FAILED,
            CHECKOUT_CREATION_SUCCESS
        }
    }
}
