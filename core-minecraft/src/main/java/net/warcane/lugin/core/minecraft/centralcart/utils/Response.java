package net.warcane.lugin.core.minecraft.centralcart.utils;

public record Response(boolean isSuccessful, int statusCode, String body) { }
