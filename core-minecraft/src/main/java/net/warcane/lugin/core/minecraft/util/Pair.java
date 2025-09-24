package net.warcane.lugin.core.minecraft.util;

/**
 * @author Rok, Pedro Lucas nmm. Created on 12/08/2025
 * @project factions-essentials
 */
public record Pair<V1, V2>(V1 a, V2 b) {

    public static <V1, V2> Pair<V1, V2> of(V1 a, V2 b) {
        return new Pair<>(a, b);
    }
}
