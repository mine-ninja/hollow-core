package net.warcane.lugin.core.minecraft.util.team;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NametagTeam {

    private static final String UNIQUE_ID = UUID.randomUUID().toString().replaceAll("[^a-zA-Z]", "").toUpperCase().substring(0, 5);
    private static int ID = 0;

    private final ArrayList<String> members = new ArrayList<>();

    @Getter
    private String name;
    @Getter
    private String prefix = "";
    @Getter
    private String suffix = "";
    @Getter
    private int priority = -1;

    public NametagTeam(String prefix, String suffix, int priority) {
        this.name = UNIQUE_ID + "_" + getNameFromInput(priority) + ID;
        this.name = (this.name.length() > 16) ? this.name.substring(0, 16) : this.name;

        this.prefix = ChatColor.translateAlternateColorCodes('&', prefix);
        this.suffix = ChatColor.translateAlternateColorCodes('&', suffix);
        this.priority = priority;

        ID++; //Increase ID
    }

    public List<String> getMembers() {
        return members;
    }

    public void addMember(Player player) {
        if (this.members.contains(player.getName()))
            return;

        this.members.add(player.getName());
    }

    public void removeMember(Player player) {
        if (!this.members.contains(player.getName()))
            return;

        this.members.remove(player.getName());
    }

    private String getNameFromInput(int input) {
        if (input < 10) {
            return "00" + input;
        } else if (input < 100) {
            return "0" + input;
        } else {
            return String.valueOf(input);
        }
    }

    public boolean isSimilar(String prefix, String suffix) {
        return (this.prefix.equals(prefix) && this.suffix.equals(suffix));
    }

}
