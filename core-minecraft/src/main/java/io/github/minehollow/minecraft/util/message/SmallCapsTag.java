package io.github.minehollow.minecraft.util.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import net.kyori.adventure.text.minimessage.tree.Node;


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
