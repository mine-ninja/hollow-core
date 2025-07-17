package net.warcane.lugin.core.util.property;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class Property {


    @NotNull
    public static String getOrThrow(@NotNull String key, @NotNull Supplier<Throwable> exceptionSupplier) {
        var value = get(key);
        if (value == null) {
            try {
                throw exceptionSupplier.get();
            } catch (Throwable e) {
                throw new RuntimeException("Property '" + key + "' not found in environment or system properties.", e);
            }
        }
        return value;

    }

    @NotNull
    public static String getOrThrow(@NotNull String key) {
        var value = get(key);
        if (value == null) {
            throw new IllegalArgumentException("Property '" + key + "' not found in environment or system properties.");
        }
        return value;
    }

    /**
     * Obtém o valor de uma propriedade do sistema, seja do ambiente ou da propriedade do sistema.
     *
     * @param key a chave da propriedade a ser obtida
     * @return o valor da propriedade, ou null se a propriedade não existir
     */
    @Nullable
    public static String get(@NotNull String key) {
        var env = System.getenv(key);
        if (env == null) {
            env = System.getProperty(key);
        }
        return env;
    }

    /**
     * Obtém o valor de uma propriedade do sistema, seja do ambiente ou da propriedade do sistema.
     *
     * @param key          a chave da propriedade a ser obtida
     * @param defaultValue o valor padrão a ser usado caso a propriedade não exista
     * @return o valor da propriedade, ou o valor padrão fornecido se a propriedade não existir
     */
    @NotNull
    public static String get(@NotNull String key, @NotNull String defaultValue) {
        return get(key, () -> defaultValue);
    }

    /**
     * Obtém o valor de uma propriedade do sistema como um booleano, seja do ambiente ou da propriedade do sistema.
     *
     * @param key          a chave da propriedade a ser obtida
     * @param defaultValue o valor padrão a ser usado caso a propriedade não exista
     * @return o valor da propriedade como booleano, ou o valor padrão fornecido se a propriedade não existir
     */
    public static boolean getBoolean(@NotNull String key, boolean defaultValue) {
        var value = get(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Obtém o valor de uma propriedade do sistema, seja do ambiente ou da propriedade do sistema.
     *
     * @param key                  a chave da propriedade a ser obtida
     * @param defaultValueSupplier um fornecedor de valor padrão a ser usado caso a propriedade não exista
     * @return o valor da propriedade, ou o valor padrão fornecido se a propriedade não existir
     */
    public static String get(@NotNull String key, @NotNull Supplier<String> defaultValueSupplier) {
        var env = System.getenv(key);
        if (env == null) {
            env = System.getProperty(key);
            if (env == null) {
                env = defaultValueSupplier.get();
            }
        }

        return env == null ? defaultValueSupplier.get() : env;
    }
}
