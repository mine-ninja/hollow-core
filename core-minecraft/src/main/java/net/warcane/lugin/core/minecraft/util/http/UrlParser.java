package net.warcane.lugin.core.minecraft.util.http;

import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlParser {

    public static URL parseUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException exception) {
            throw new RuntimeException(exception);
        }
    }
}
