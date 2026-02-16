package io.github.minehollow.bestiary.custom;

import io.github.minehollow.bestiary.BestiaryPlugin;
import io.github.minehollow.bestiary.archetype.MobArchetype;
import io.github.minehollow.bestiary.spawner.CustomMobSpawner;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class CustomMobManager {

    private final BestiaryPlugin plugin;
    private final Map<UUID, CustomMobEntity> cachedEntities = new Object2ObjectOpenHashMap<>();

    public CustomMobEntity getEntity(@NotNull Entity entity) {
        return cachedEntities.get(entity.getUniqueId());
    }

    public void spawnEntity(
        @NotNull MobArchetype archetype,
        @NotNull Location location,
        @Nullable CustomMobSpawner spawner
    ) {
        final var customEntity = new CustomMobEntity(
            location,
            archetype.entityType(),
            archetype.levelRange().getRandomValue(),
            spawner
        );

        customEntity.setHealthPerLevel(archetype.healthPerLevel());
        customEntity.setDamagePerLevel(archetype.damagePerLevel());
        customEntity.updateMetadata(archetype.displayName());

        cachedEntities.put(customEntity.getBukkitEntity().getUniqueId(), customEntity);
    }

    public void tick() {
        var iterator = cachedEntities.values().iterator();
        while (iterator.hasNext()) {
            var entity = iterator.next();

            entity.tickActive();

            if (entity.isInactive() || !entity.getBukkitEntity().isValid()) {
                entity.remove();
                iterator.remove();
                continue;
            }

            if (entity.getSourceSpawner() != null) {
                var arch = plugin.getMobArchetypeService().getById(entity.getSourceSpawner().getArchetypeId());
                if (arch != null) {
                    entity.updateMetadata(arch.displayName());
                }
            }
        }
    }

    public void releaseEntity(@NotNull UUID entityId) {
        final var removed = cachedEntities.remove(entityId);
        if (removed != null) removed.remove();
    }

    private Listener createInternalListener() {
        return new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            void handleDeath(EntityDeathEvent event) {
                final var customEntity = getEntity(event.getEntity());
                if (customEntity != null) {

                }
            }
        };
    }
}
