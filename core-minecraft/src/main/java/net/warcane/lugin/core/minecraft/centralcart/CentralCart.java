package net.warcane.lugin.core.minecraft.centralcart;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import io.socket.client.IO;
import io.socket.client.Socket;
import net.kyori.adventure.text.Component;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.centralcart.models.Order;
import net.warcane.lugin.core.minecraft.centralcart.models.Product;
import net.warcane.lugin.core.minecraft.centralcart.utils.QueuedCommand;
import net.warcane.lugin.core.minecraft.centralcart.utils.Response;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.util.property.Property;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class CentralCart {
    @Getter
	private final Logger logger = LoggerFactory.getLogger(CentralCart.class);
	
	private final String token;
	private final OkHttpClient client;
	private Socket socket;
	
	@Getter private final Map<Integer, Product> productMap = Maps.newConcurrentMap();
	
	public CentralCart() {
		this.client = new OkHttpClient.Builder()
			.connectTimeout(10L, TimeUnit.SECONDS)
			.readTimeout(10L, TimeUnit.SECONDS)
			.writeTimeout(10L, TimeUnit.SECONDS)
			.build();
		
		this.token = Property.get("CENTRAL_CART_TOKEN", "");
		if (this.token.isEmpty()) {
			logger.warn("O token de autenticação da API do CentralCart não foi definido. Verifique a variável de ambiente 'CENTRAL_CART_TOKEN'.");
		}
	}
	
	public boolean isTokenValid() {
		return !this.token.isEmpty();
	}
	
	public Response perform(final String endpoint, final String method, final JsonObject... params) {
		final Request.Builder request = new Request.Builder().url(endpoint)
			.header("Authorization", "Bearer " + this.token)
			.header("Content-Type", "application/json")
			.method(method, (params.length > 0) ? RequestBody.create(MediaType.parse("application/json"), params[0].toString()) : null);
		
		try (okhttp3.Response response = this.client.newCall(request.build()).execute()) {
			Response finalResponse;
			try {
				String body = (response.body() != null) ? response.body().string() : "";
				finalResponse = new Response(response.isSuccessful(), response.code(), body);
			} catch (Throwable t) {
				try {
					response.close();
				} catch (Throwable exception) {
					t.addSuppressed(exception);
				}
				throw t;
			}
			
			if (response != null) {
				response.close();
			}
			
			return finalResponse;
		}
		catch (IOException e) {
			logger.debug("Não foi possível estabelecer uma conexão com a API.");
			logger.debug(e.getMessage());
			
			for (final StackTraceElement element : e.getStackTrace()) {
				logger.debug(element.toString());
			}
			
			return null;
		}
	}
	
	public void loadProducts() {
		Response response = perform("https://api.centralcart.com.br/v1/app/package", "GET", new JsonObject[0]);
		if (response == null || response.statusCode() >= 500) {
			logger.info("An error occurred while trying to get the product list from the website!");
			return;
		}
		if (response.statusCode() == 401) {
			logger.info("An error occurred while connecting to your store. Please check that the token is correct.");
			return;
		}
		if (!response.isSuccessful()) {
			return;
		}
		
		JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonObject().get("data").getAsJsonArray();
		Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer()).create();
		
		this.productMap.clear();
		jsonArray.forEach(jsonElement -> {
            Product product = gson.fromJson(jsonElement, Product.class);
            /*JsonObject jsonObject = jsonElement.getAsJsonObject();
			
			Product.ProductBuilder builder = Product.builder();
			
			int id = jsonObject.get("id").getAsInt();
			builder.id(id);
			builder.enabled(jsonObject.get("enabled").getAsBoolean());
			builder.price(jsonObject.get("price").getAsDouble());
			builder.name(jsonObject.get("name").getAsString());
			builder.slug(jsonObject.get("slug").getAsString());
			
			if (jsonObject.has("description") && !jsonObject.get("description").isJsonNull()) {
				builder.description(jsonObject.get("description").getAsString().replaceAll("\\n", "<newline>"));
			}
			if (jsonObject.has("category_id") && !jsonObject.get("category_id").isJsonNull()) {
				builder.categoryId(jsonObject.get("category_id").getAsInt());
			}
			builder.priceDisplay(jsonObject.get("price_display").getAsString());*/
			this.productMap.put(product.id(), product);
		});
        
        logger.info("Foram carregados {} produtos do CentralCart.", this.productMap.size());
	}
	
	public void initSocket() {
		Map<String, List<String>> stringListMap = new HashMap<>();
		stringListMap.put("Authorization", Collections.singletonList("Bearer " + this.token));
		stringListMap.put("x-extension", Collections.singletonList("plugin"));
		IO.Options options = IO.Options.builder().setExtraHeaders(stringListMap).build();
		this.socket = IO.socket(URI.create("wss://ws.centralcart.com.br"), options);
		
		this.socket.on("EXECUTE_COMMAND", args -> {
			assert args[0] != null;
			
			QueuedCommand[] queuedCommand = new QueuedCommand[]{ (new Gson()).fromJson(args[0].toString(), QueuedCommand.class) };
			for (QueuedCommand command : queuedCommand) {
				if (command.userId() == null) return;
				
				Player player = Bukkit.getPlayer(command.userId());
				if (player != null || command.offlineExecute()) {
					Tasks.runSync(() -> Bukkit.getServer().getPluginManager().callEvent(new OrderActivatedEvent(command.userId(), command.order())));
				}
			}
		});
		logger.info("Conectando com o provedor de entregas...");
		this.socket.connect();
	}
	
	public void disableSocket() {
		this.socket.disconnect();
	}
    
    public List<Product> getProducts() {
        return Lists.newArrayList(this.productMap.values());
    }
	
	public static class LocalDateTimeDeserializer implements JsonDeserializer<LocalDateTime> {
		@Override
		public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			String dateString = json.getAsString();
			return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
		}
	}
}
