package net.warcane.lugin.core.minecraft.centralcart.utils;

import net.warcane.lugin.core.minecraft.centralcart.models.Order;
import com.google.gson.annotations.SerializedName;

public record QueuedCommand(
	@SerializedName("id") long id,
	@SerializedName("store_id") long storeId,
	@SerializedName("user_id") String userId,
	@SerializedName("command_id") String command,
	@SerializedName("offline_execute") boolean offlineExecute,
	Order order
) { }
