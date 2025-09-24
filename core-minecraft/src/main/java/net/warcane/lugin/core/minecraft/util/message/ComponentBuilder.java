package net.warcane.lugin.core.minecraft.util.message;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;

/**
 * @author Rok, Pedro Lucas nmm. Created on 15/09/2025
 * @project punish
 */
public class ComponentBuilder {

    private Component msg;

    public static ComponentBuilder of() {
        return new ComponentBuilder();
    }

    public ComponentBuilder() {
        this.msg = Component.text("");
    }

    public ComponentBuilder simple(String msg) {
        this.msg = this.msg.append(StringUtils.text(msg));
        return this;
    }

    public ComponentBuilder simpleAction(String msg, ClickCallback<Audience> action) {
        this.msg = this.msg.append(StringUtils.text(msg).clickEvent(ClickEvent.callback(action, ClickCallback.Options.builder().uses(1).build())));
        return this;
    }

    public ComponentBuilder simpleHover(String msg, String hover) {
        this.msg = this.msg.append(StringUtils.text(msg).hoverEvent(HoverEvent.showText(StringUtils.text(hover))));
        return this;
    }

    public ComponentBuilder hover(String msg, String... hover) {
        Component hoverComp = Component.text("");
        for (String s : hover) {
            hoverComp = hoverComp.append(StringUtils.text(s)).append(Component.text("\n"));
        }
        this.msg = this.msg.append(StringUtils.text(msg).clickEvent(ClickEvent.runCommand("")).hoverEvent(HoverEvent.showText(hoverComp)));
        return this;
    }

    public ComponentBuilder actionHover(String msg, ClickCallback<Audience> action, String hover) {
        this.msg = this.msg.append(StringUtils.text(msg).clickEvent(ClickEvent.callback(action, ClickCallback.Options.builder().uses(1).build())).hoverEvent(HoverEvent.showText(StringUtils.text(hover))));
        return this;
    }

    public ComponentBuilder actionHover(String msg, ClickCallback<Audience> action, String... hover) {
        Component hoverComp = getHover(hover);
        this.msg = this.msg.append(StringUtils.text(msg).clickEvent(ClickEvent.callback(action, ClickCallback.Options.builder().uses(1).build())).hoverEvent(HoverEvent.showText(hoverComp)));
        return this;
    }

    private Component getHover(String[] hover) {
        Component hoverComp = Component.text("");
        int length = hover.length;
        for (int i = 0; i < length; i++) {
            String s = hover[i];
            hoverComp = hoverComp.append(StringUtils.text(s));
            if (i < length - 1) {
                hoverComp = hoverComp.append(Component.text("\n"));
            }
        }
        return hoverComp;
    }

    public ComponentBuilder suggestHover(String msg, String command, String... hover) {
        Component hoverComp = getHover(hover);
        this.msg = this.msg.append(StringUtils.text(msg).clickEvent(ClickEvent.suggestCommand(command)).hoverEvent(HoverEvent.showText(hoverComp)));
        return this;
    }

    public ComponentBuilder clipboardHover(String msg, String clipboard, String... hover) {
        Component hoverComp = getHover(hover);
        this.msg = this.msg.append(StringUtils.text(msg).clickEvent(ClickEvent.copyToClipboard(clipboard)).hoverEvent(HoverEvent.showText(hoverComp)));
        return this;
    }

    public ComponentBuilder linkHover(String msg, String link, String... hover) {
        Component hoverComp = getHover(hover);
        this.msg = this.msg.append(StringUtils.text(msg).clickEvent(ClickEvent.openUrl(link)).hoverEvent(HoverEvent.showText(hoverComp)));
        return this;
    }

    public ComponentBuilder newLine() {
        this.msg = this.msg.append(Component.text("\n"));
        return this;
    }

    public Component build() {
        return this.msg;
    }

    public void send(Audience audience) {
        audience.sendMessage(this.msg);
    }
}
