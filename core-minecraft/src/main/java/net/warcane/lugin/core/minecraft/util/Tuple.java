package net.warcane.lugin.core.minecraft.util;

/**
 * @author Rok, Pedro Lucas nmm. Created on 23/08/2025
 * @project punish
 */
public record Tuple<K, V>(K a, V b) {

    public static <V1, V2> Tuple<V1, V2> of(V1 a, V2 b) {
        return new Tuple<>(a, b);
    }

}
