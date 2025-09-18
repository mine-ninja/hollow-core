package net.warcane.lugin.core.minecraft.centralcart.models;

import com.google.gson.annotations.SerializedName;

import lombok.Builder;

@Builder
public record Product(
	@SerializedName("id") int id,
	@SerializedName("enabled") boolean enabled,
	@SerializedName("price") double price,
	
	@SerializedName("name") String name,
	@SerializedName("description") String description,
	@SerializedName("slug") String slug,
	
	@SerializedName("category_id") int categoryId,
	@SerializedName("price_display") String priceDisplay
) {
	public String getPriceDisplay() {
		return this.priceDisplay.replace(' ', ' ');
	}
}
