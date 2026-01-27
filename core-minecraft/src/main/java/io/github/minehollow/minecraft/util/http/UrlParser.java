package io.github.minehollow.minecraft.util.http;

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
