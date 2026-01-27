package io.github.minehollow.minecraft.util;


public record Pair<V1, V2>(V1 a, V2 b) {

    public static <V1, V2> Pair<V1, V2> of(V1 a, V2 b) {
        return new Pair<>(a, b);
    }
}
