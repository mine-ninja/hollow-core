package net.warcane.lugin.core.minecraft.skin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import lombok.Data;
import net.warcane.lugin.core.minecraft.util.GameProfileUtil;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Skin {

    private static final JsonParser JSON_PARSER = new JsonParser();

    private String name;
    private String uuid;
    private String valueSkin;
    private String signatureSkin;

    public Skin() {
    }

    public Skin(Player player) throws Exception {
        try {
            PropertyMap properties = GameProfileUtil.getPropertyMap(player);
            Collection<Property> textures = properties.get("textures");
            for (Property texture : textures) {
                this.name = texture.getName();
                this.valueSkin = texture.getValue();
                this.signatureSkin = texture.getSignature();
                break;
            }
        } catch (Throwable throwable) {
            throw new Exception(throwable);
        }
    }

    public Skin(Property property) {
        this(property.getValue(), property.getSignature());
    }

    public Skin(String name, String valueSkin, String signatureSkin) {
        this(valueSkin, signatureSkin);
        this.name = name;
    }

    public Skin(UUID uuid, String valueSkin, String signatureSkin) {
        this(valueSkin, signatureSkin);
        this.uuid = uuid.toString();
    }

    public Skin(String valueSkin, String signatureSkin) {
        this.valueSkin = valueSkin;
        this.signatureSkin = signatureSkin;
    }

    public void setPropertiesSkin(String propertiesSkin) {
        JsonObject json = JSON_PARSER.parse(propertiesSkin).getAsJsonObject();
        if (this.name != null) {
            this.name = json.get("name").getAsString();
        }
        this.valueSkin = json.get("value").getAsString();
        this.signatureSkin = json.get("signature").getAsString();
    }

    public Property getProperty() {
        return new Property("textures", getValueSkin(), getSignatureSkin());
    }

    public String getPropertiesSkin() {
        return "{" +
               "    \"name\" : \"textures\"," +
               "    \"value\" : \"" + valueSkin + "\"," +
               "    \"signature\" : \"" + signatureSkin + "\"" +
               "  }";
    }

}
