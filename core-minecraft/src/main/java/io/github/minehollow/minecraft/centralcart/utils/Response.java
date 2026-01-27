package io.github.minehollow.minecraft.centralcart.utils;

public record Response(boolean isSuccessful, int statusCode, String body) { }
