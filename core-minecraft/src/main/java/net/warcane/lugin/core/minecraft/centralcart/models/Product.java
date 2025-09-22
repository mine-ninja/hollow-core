package net.warcane.lugin.core.minecraft.centralcart.models;

import com.google.gson.annotations.SerializedName;

import lombok.Builder;
import java.util.ArrayList;
import java.util.List;

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
    public List<String> getDescriptionLines() {
        String desc = this.description.replaceAll("<[^>]*>", "");
        
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : desc.split("\\s+")) {
            if (currentLine.length() + word.length() + 1 > 40) {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
            }
            if (!currentLine.isEmpty()) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }
        
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
	public String getPriceDisplay() {
		return this.priceDisplay.replace(' ', ' ');
	}
}
