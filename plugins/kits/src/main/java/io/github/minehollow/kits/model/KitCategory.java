package io.github.minehollow.kits.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bukkit.Material;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KitCategory {
    @BsonId
    private String id;
    private String displayName;
    private Material icon;
    private int priority;

    public KitCategory(String id, String displayName, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.priority = 0;
    }
}
