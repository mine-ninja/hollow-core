package net.warcane.lugin.core.group;

import com.mongodb.client.model.Indexes;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.util.data.MongoRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Slf4j
public class GroupPermissionService {

    private static final String CACHE_KEY = "groupperms";

    private final Map<String, GroupPermissionSet> localCache = new ConcurrentHashMap<>();
    private final MongoRepository<String, GroupPermissionSet> permissionsRepository = new MongoRepository<>(GroupPermissionSet.class, "groupId");

    /**
     * ExecutorService para operações assíncronas.
     */
    private final ExecutorService executorService;

    public GroupPermissionService(@NotNull ExecutorService executorService) {
        this.executorService = executorService;
        this.permissionsRepository.useCollection(collection -> collection.createIndex(Indexes.hashed("groupId")));
    }

    /**
     * Obtém o conjunto de permissões em cache para um grupo de jogadores.
     *
     * @param group o grupo de jogadores cujas permissões serão obtidas.
     * @return o conjunto de permissões do grupo, ou null se não estiver em cache.
     */
    @Nullable
    public GroupPermissionSet getCachedPermissionsForGroup(@NotNull PlayerGroup group) {
        return localCache.get(group.getId());
    }

    /**
     * Obtém o conjunto de permissões em cache para um grupo de jogadores.
     *
     * @param group o grupo de jogadores cujas permissões serão obtidas.
     * @return o conjunto de permissões do grupo, ou lança uma exceção se não estiver em cache.
     * @throws NullPointerException se as permissões do grupo não forem encontradas no cache.
     */
    @NotNull
    public GroupPermissionSet getCachedPermissionsForGroupOrThrow(@NotNull PlayerGroup group) {
        return Objects.requireNonNull(
          getCachedPermissionsForGroup(group),
          "Group permissions not found in cache for group: " + group.getId()
        );
    }

    /**
     * Obtém o conjunto de permissões de um grupo de jogadores.
     *
     * @param group o grupo de jogadores cujas permissões serão obtidas.
     * @return uma CompletableFuture que será completada com o conjunto de permissões do grupo.
     * @throws IllegalStateException se as permissões do grupo não forem encontradas.
     */
    public CompletableFuture<@NotNull GroupPermissionSet> getGroupPermissionSet(@NotNull PlayerGroup group) {
        final var locallyCached = this.getCachedPermissionsForGroup(group);
        if (locallyCached != null) {
            log.info("Returning locally cached permissions for group: {}", group.getId());
            return CompletableFuture.completedFuture(locallyCached);
        }

        return supplyAsync(() -> {
            log.info("Permissions for group {} not found in Redis, checking MongoDB...", group.getId());
            var fromMongo = permissionsRepository.findById(group.getId());
            if (fromMongo != null) {
                localCache.put(group.getId(), fromMongo);
                log.info("Loaded permissions for group {} from MongoDB and updated caches: {}", group.getId(), fromMongo.permissions());
                return fromMongo;
            }

            throw new IllegalStateException("Group permissions not found for group: " + group.getId());
        }, executorService);
    }

    /**
     * Carrega as permissões de um grupo de jogadores.
     *
     * @param group             o grupo de jogadores cujas permissões serão carregadas.
     * @param createIfNotExists se verdadeiro, cria um novo conjunto de permissões se não existir.
     * @return uma CompletableFuture que será completada com o conjunto de permissões do grupo.
     * @throws IllegalStateException se as permissões do grupo não forem encontradas throwable {@param createIfNotExists} for falso.
     */
    public CompletableFuture<@NotNull GroupPermissionSet> loadPermissions(@NotNull PlayerGroup group, boolean createIfNotExists) {
        return supplyAsync(() -> {

            var fromDb = permissionsRepository.findById(group.getId());
                if(fromDb != null){
                    log.info("Loaded permissions for group {} from MongoDB: {}", group.getId(), fromDb.permissions());
                } else {
                    log.info("No permissions found for group {} in MongoDB.", group.getId());
                }


            if (fromDb != null) {
                log.info("Loaded permissions for group: {}", group.getId());
                localCache.put(group.getId(), fromDb);
                log.info("Permissions for group {}: {}", group.getId(), fromDb.permissions());
                return fromDb;
            }

            if (createIfNotExists) {
                log.info("Creating new permissions for group: {}", group.getId());

                final var newPermissions = GroupPermissionSet.create(group);
                permissionsRepository.save(newPermissions, GroupPermissionSet::groupId);
                localCache.put(group.getId(), newPermissions);

                log.info("Created new permissions for group {} and permissions {}", group.getId(), newPermissions.permissions());
                return newPermissions;
            }

            throw new IllegalStateException("Group permissions not found for group: " + group.getId());
        }, executorService);
    }

    /**
     * Carrega as permissões de todos os grupos de jogadores.
     *
     * @return uma CompletableFuture que será completada quando todas as permissões forem carregadas.
     */
    public CompletableFuture<Void> loadPermissionsForAllGroups() {
        return allOf(Arrays.stream(PlayerGroup.values())
          .map(group -> loadPermissions(group, true))
          .toArray(CompletableFuture[]::new));
    }


    /**
     * Atualiza o conjunto de permissões de um grupo de jogadores.
     *
     * @param permissionSet o conjunto de permissões a ser atualizado.
     * @return uma CompletableFuture que será completada com o conjunto de permissões atualizado.
     * @throws IllegalStateException se a atualização falhar.
     */
    public CompletableFuture<@NotNull GroupPermissionSet> updateGroupPermissionSet(@NotNull GroupPermissionSet permissionSet) {
        return supplyAsync(() -> {
            final var updated = permissionsRepository.save(permissionSet, GroupPermissionSet::groupId);
            if (updated == null) {
                throw new IllegalStateException("Failed to update group permissions: " + permissionSet.groupId());
            }

            log.info("Updated group permissions for group: {}", permissionSet.groupId());
            localCache.put(permissionSet.groupId(), updated);

            log.info("Updated local cache for group: {} with permissions: {}", permissionSet.groupId(), updated.permissions());
            return updated;
        }, executorService);
    }
}
