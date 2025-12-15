package net.warcane.lugin.core.minecraft.util.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import net.kyori.adventure.text.minimessage.tree.Node;

/**
 * @author Rok, Pedro Lucas nmm. Created on 15/12/2025
 * @project lugin-core
 */
public class SmallCapsTag implements Modifying {

    @Override
    public void visit(Node node, int depth) {
    }

    @Override
    public Component apply(Component component, int depth) {
        return SmallCapsMapping.toSmallCaps(component);
    }

    @Override
    public void postVisit() {
    }
}
