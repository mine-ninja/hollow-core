package io.github.minehollow.minecraft.centralcart.models;

import com.google.gson.annotations.SerializedName;

public record Order(
	@SerializedName("client_identifier") String clientIdentifier,
	@SerializedName("client_email") String clientEmail,
	@SerializedName("client_name") String clientName,
	@SerializedName("client_discord") String clientDiscord,
	@SerializedName("internal_id") String internalId
) { }
