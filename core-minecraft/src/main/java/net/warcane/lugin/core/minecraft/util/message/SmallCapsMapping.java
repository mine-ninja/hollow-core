package net.warcane.lugin.core.minecraft.util.message;

import it.unimi.dsi.fastutil.chars.Char2CharArrayMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;


/**
 * @author Rok, Pedro Lucas nmm. Created on 04/08/2025
 * @project lugin-core
 */
public class SmallCapsMapping {

    private static final Char2CharArrayMap SMALL_CAPS_MAPPING = new Char2CharArrayMap() {{
        put('a', 'ᴀ');
        put('b', 'ʙ');
        put('c', 'ᴄ');
        put('d', 'ᴅ');
        put('e', 'ᴇ');
        put('f', 'ꜰ');
        put('g', 'ɢ');
        put('h', 'ʜ');
        put('i', 'ɪ');
        put('j', 'ᴊ');
        put('k', 'ᴋ');
        put('l', 'ʟ');
        put('m', 'ᴍ');
        put('n', 'ɴ');
        put('o', 'ᴏ');
        put('p', 'ᴘ');
        put('q', 'ǫ');
        put('r', 'ʀ');
        put('s', 's');
        put('t', 'ᴛ');
        put('u', 'ᴜ');
        put('v', 'ᴠ');
        put('w', 'ᴡ');
        put('x', 'x');
        put('y', 'ʏ');
        put('z', 'z');
    }};

    public static String toSmallCaps(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (char c : text.toCharArray())
            result.append(convertCharToSmallCaps(c));
        return result.toString();
    }

    public static Component toSmallCaps(Component component) {
        if (!(component instanceof TextComponent textComp)) {
            return component;
        }
        return Component.text(SmallCapsMapping.toSmallCaps(textComp.content()))
            .style(textComp.style());
    }

    private static char convertCharToSmallCaps(char c) {
        return SMALL_CAPS_MAPPING.getOrDefault(Character.valueOf(Character.toLowerCase(c)), Character.valueOf(c));
    }
}
