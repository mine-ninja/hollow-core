package net.warcane.lugin.core.minecraft.centralcart.models;

import com.google.gson.annotations.SerializedName;

import lombok.Builder;
import java.util.List;

@Builder
public record Coupon(
	int id,
	
	String coupon,
	String type,
	int value,
	int uses,
	
	@SerializedName("store_id") int storeId,
	@SerializedName("applies_to") List<Integer> appliesTo,
	@SerializedName("max_uses") Integer maxUses,
	@SerializedName("expires_in") String expiresIn,
	@SerializedName("limit_criteria") String limitCriteria,
	@SerializedName("created_at") String createdAt,
	@SerializedName("updated_at") String updatedAt
) { }
