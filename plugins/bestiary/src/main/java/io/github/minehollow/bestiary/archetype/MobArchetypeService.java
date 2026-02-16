package io.github.minehollow.bestiary.archetype;


import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.sdk.util.data.MongoRepository;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class MobArchetypeService {

    private final MongoRepository<String, MobArchetype> repository = new MongoRepository<>(MobArchetype.class);
    private final Map<String, MobArchetype> cache = new ConcurrentHashMap<>();

    public void loadAllArchetypes() {
        Tasks.runAsync(() -> {
            final var archetypes = repository.queryAll();
            archetypes.forEach(archetype -> cache.put(archetype.id(), archetype));

            Bukkit.getConsoleSender().sendMessage("§a[Bestiary] §7Carregados §a" + archetypes.size() + " §7archetypes de mobs.");
        });
    }

    public @Nullable MobArchetype getById(@NotNull String id) {
        return cache.get(id);
    }

    public Collection<MobArchetype> getAllCached() {
        return cache.values();
    }

    public List<MobArchetype> selectAll() {
        return repository.queryAll();
    }

    public void save(@NotNull MobArchetype archetype) {
        Tasks.runAsync(() -> {
            final var saved = repository.save(archetype, MobArchetype::id);
            cache.put(saved.id(), saved);
        });
    }

    public void delete(@NotNull String id) {
        Tasks.runAsync(() -> {
            repository.deleteById(id);
            cache.remove(id);
        });
    }

    public void fetchById(
        @NotNull String id,
        @NotNull BiConsumer<MobArchetype, Throwable> callback
    ) {
        Tasks.runAsync(() -> {
            try {
                final var archetype = cache.computeIfAbsent(id, repository::findById);
                callback.accept(archetype, null);
            } catch (Throwable t) {
                callback.accept(null, t);
            }
        });
    }

}
