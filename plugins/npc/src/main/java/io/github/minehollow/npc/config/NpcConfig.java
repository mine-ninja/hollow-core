package io.github.minehollow.npc.config;

import org.bukkit.Location;
import java.util.ArrayList;
import java.util.List;
import io.github.minehollow.npc.api.NpcAction;
import io.github.minehollow.npc.api.NpcClickType;

public class NpcConfig {
    private String id;
    private Location location;
    private String skinValue;
    private String skinSignature;
    private double scale = 1.0;
    private List<String> hologramLines = new ArrayList<>();
    private double hologramOffset = 2.2;
    private List<NpcAction> actions = new ArrayList<>();
    private NpcClickType clickType = NpcClickType.RIGHT;
    private boolean lookAtNearestPlayer = false;

    public NpcConfig(String id, Location location) {
        this.id = id;
        this.location = location;
    }

    public NpcConfig(String id, Location location, String skinValue, String skinSignature,
                     double scale, List<String> hologramLines, double hologramOffset,
                     List<NpcAction> actions, NpcClickType clickType, boolean lookAtNearestPlayer) {
        this.id = id;
        this.location = location;
        this.skinValue = skinValue;
        this.skinSignature = skinSignature;
        this.scale = scale;
        this.hologramLines = new ArrayList<>(hologramLines);
        this.hologramOffset = hologramOffset;
        this.actions = new ArrayList<>(actions);
        this.clickType = clickType;
        this.lookAtNearestPlayer = lookAtNearestPlayer;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public String getSkinValue() { return skinValue; }
    public void setSkin(String value, String signature) { this.skinValue = value; this.skinSignature = signature; }
    public String getSkinSignature() { return skinSignature; }
    public double getScale() { return scale; }
    public void setScale(double scale) { this.scale = scale; }
    public List<String> getHologramLines() { return hologramLines; }
    public double getHologramOffset() { return hologramOffset; }
    public void setHologramOffset(double offset) { this.hologramOffset = offset; }
    public List<NpcAction> getActions() { return actions; }
    public void setActions(List<NpcAction> actions) { this.actions = actions; }
    public NpcClickType getClickType() { return clickType; }
    public void setClickType(NpcClickType type) { this.clickType = type; }
    public boolean isLookAtNearestPlayer() { return lookAtNearestPlayer; }
    public void setLookAtNearestPlayer(boolean look) { this.lookAtNearestPlayer = look; }
}
