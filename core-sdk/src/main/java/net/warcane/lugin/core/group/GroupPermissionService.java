package net.warcane.lugin.core.group;

import net.warcane.lugin.core.util.data.MongoRepository;
import net.warcane.lugin.core.util.data.RedisCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;

public class GroupPermissionService {

    private static final String CACHE_KEY = "groupperms";

    private final Map<String, GroupPermissionSet> localCache = new ConcurrentHashMap<>();
    private final RedisCache<GroupPermissionSet> redisCache = new RedisCache<>(GroupPermissionSet.class);
    private final MongoRepository<String, GroupPermissionSet> permissionsRepository = new MongoRepository<>(
      GroupPermissionSet.class,
      "uniqueId"
    );

    /**
     * ExecutorService para operações assíncronas.
     */
    private final ExecutorService executorService;

    public GroupPermissionService(@NotNull ExecutorService executorService) {
        this.executorService = executorService;
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
            return CompletableFuture.completedFuture(locallyCached);
        }

        return supplyAsync(() -> {
            final Supplier<GroupPermissionSet> supplier = () -> permissionsRepository.findById(group.getId());
            final var fromDb = redisCache.hget(CACHE_KEY, group.getId(), supplier);
            if (fromDb != null) {
                localCache.put(group.getId(), fromDb);
                return fromDb;
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
     * @throws IllegalStateException se as permissões do grupo não forem encontradas e {@param createIfNotExists} for falso.
     */
    public CompletableFuture<@NotNull GroupPermissionSet> loadPermissions(@NotNull PlayerGroup group, boolean createIfNotExists) {
        return supplyAsync(() -> {
            final Supplier<GroupPermissionSet> supplier = () -> permissionsRepository.findById(group.getId());
            final var fromDb = redisCache.hget(CACHE_KEY, group.getId(), supplier);
            if (fromDb != null) {
                return fromDb;
            }

            if (createIfNotExists) {
                final var newPermissions = GroupPermissionSet.create(group);
                permissionsRepository.save(newPermissions, GroupPermissionSet::groupId);
                redisCache.hset(CACHE_KEY, group.getId(), newPermissions);
                localCache.put(group.getId(), newPermissions);
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

            redisCache.hset(CACHE_KEY, permissionSet.groupId(), updated);
            localCache.put(permissionSet.groupId(), updated);
            return updated;
        }, executorService);
    }
}
