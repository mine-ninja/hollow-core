package net.warcane.lugin.core.minecraft.centralcart.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import lombok.Builder;
import java.lang.reflect.Type;
import java.util.List;

@Builder
public record Coupon(
    @SerializedName("id") int id,
    
    @SerializedName("coupon") String coupon,
    @SerializedName("type") String type,
    @SerializedName("value") double value,
	
	@SerializedName("store_id") int storeId,
	@SerializedName("applies_to") List<Integer> appliesTo,
	@SerializedName("max_uses") Integer maxUses,
	@SerializedName("expires_in") String expiresIn,
	@SerializedName("created_at") String createdAt,
	@SerializedName("updated_at") String updatedAt
) {
    public static final Deserializer DESERIALIZER = new Deserializer();
    
    public static class Deserializer implements JsonDeserializer<Coupon> {
        @Override
        public Coupon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject()) {
                throw new JsonParseException("Expected JSON object");
            }
            
            JsonObject obj = json.getAsJsonObject();
            
            JsonArray applies = JsonParser.parseString(obj.get("applies_to").getAsString()).getAsJsonArray();
            List<Integer> appliesTo = context.deserialize(applies, List.class);
            
            return Coupon.builder()
                .id(obj.get("id").getAsInt())
                .coupon(obj.get("coupon").getAsString())
                .type(obj.get("type").getAsString())
                .value(obj.get("value").getAsDouble())
                .storeId(obj.get("store_id").getAsInt())
                .appliesTo(appliesTo)
                .maxUses(obj.has("max_uses") && !obj.get("max_uses").isJsonNull() ? obj.get("max_uses").getAsInt() : null)
                .expiresIn(obj.has("expires_in") && !obj.get("expires_in").isJsonNull() ? obj.get("expires_in").getAsString() : null)
                .createdAt(obj.get("created_at").getAsString())
                .updatedAt(obj.get("updated_at").getAsString())
                .build();
            
        }
    }
}
