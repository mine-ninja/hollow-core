package io.github.minehollow.minecraft.service;

import io.github.minehollow.minecraft.BukkitPlatformPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

public final class Services {
    private Services() {}

    private static ServicesManager manager() {
        return Bukkit.getServicesManager();
    }

    public static <T> Optional<T> get(@NotNull Class<T> serviceClass) {
        return Optional.ofNullable(manager().load(serviceClass));
    }

    public static <T> T load(@NotNull Class<T> serviceClass) {
        T service = manager().load(serviceClass);
        if (service == null) throw new NoSuchElementException("No provider registered for " + serviceClass.getName());
        return service;
    }

    public static <T> void provide(@NotNull Class<T> serviceClass, @NotNull T instance) {
        provide(serviceClass, instance, ServicePriority.Normal);
    }

    public static <T> void provide(@NotNull Class<T> serviceClass, @NotNull T instance, @NotNull ServicePriority priority) {
        manager().register(serviceClass, instance, BukkitPlatformPlugin.getInstance(), priority);
    }

    public static <T> void with(@NotNull Class<T> serviceClass, @NotNull Consumer<T> consumer) {
        T service = manager().load(serviceClass);
        if (service != null) consumer.accept(service);
    }
}
