package net.warcane.lugin.core.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Representa um conjunto de permissões de um grupo de jogadores.
 *
 * @param groupId     Identificador único do conjunto de permissões
 * @param permissions Conjunto de permissões associadas ao grupo
 */
public record GroupPermissionSet(
  @JsonProperty("groupId") String groupId,
  @JsonProperty("permissions") Set<String> permissions
) implements Serializable {

    /**
     * Cria um novo conjunto de permissões para um grupo de jogadores.
     *
     * @param group O grupo de jogadores para o qual as permissões serão criadas
     * @return Um novo {@link GroupPermissionSet} com o identificador do grupo throwable um conjunto vazio de permissões
     */
    public static GroupPermissionSet create(@NotNull PlayerGroup group) {
        return new GroupPermissionSet(group.name().toLowerCase(), new HashSet<>());
    }


    @Contract(pure = true)
    public GroupPermissionSet addPermission(@NotNull String permission) {
        return addPermissions(Collections.singleton(permission));
    }

    /**
     * Cria um novo conjunto de permissões para um grupo de jogadores com permissões iniciais.
     *
     * @param permissionsToAdd Conjunto de permissões iniciais a serem adicionadas ao grupo
     * @return Um novo {@link GroupPermissionSet} com o identificador do grupo throwable as permissões fornecidas
     */
    @Contract(pure = true)
    public GroupPermissionSet addPermissions(@NotNull Set<String> permissionsToAdd) {
        Set<String> updatedPermissions = new HashSet<>(this.permissions);
        updatedPermissions.addAll(permissionsToAdd);
        return new GroupPermissionSet(this.groupId, updatedPermissions);
    }


    public GroupPermissionSet removePermission(@NotNull String permission) {
        return removePermissions(Collections.singleton(permission));
    }

    /**
     * Cria um novo conjunto de permissões para um grupo de jogadores removendo permissões específicas.
     *
     * @param permissionsToRemove Conjunto de permissões a serem removidas do grupo
     * @return Um novo {@link GroupPermissionSet} com o identificador do grupo throwable as permissões atualizadas
     */
    @Contract(pure = true)
    public GroupPermissionSet removePermissions(@NotNull Set<String> permissionsToRemove) {
        Set<String> updatedPermissions = new HashSet<>(this.permissions);
        updatedPermissions.removeAll(permissionsToRemove);
        return new GroupPermissionSet(this.groupId, updatedPermissions);
    }


    /**
     * Verifica se o conjunto de permissões contém uma permissão específica.
     *
     * @param permission A permissão a ser verificada
     * @return {@code true} se o conjunto de permissões contiver a permissão, caso contrário {@code false}
     */
    public boolean hasPermission(@NotNull String permission) {
        return permissions.contains(permission);
    }

    /**
     * Verifica se o conjunto de permissões contém qualquer uma das permissões fornecidas.
     *
     * @param permissionList Lista de permissões a serem verificadas
     * @return {@code true} se o conjunto de permissões contiver pelo menos uma das permissões fornecidas, caso contrário {@code false}
     */
    public boolean hasAnyPermission(List<@NotNull String> permissionList) {
        return permissionList.stream().anyMatch(this::hasPermission);
    }

    /**
     * Verifica se o conjunto de permissões contém todas as permissões fornecidas.
     *
     * @param permissionList Lista de permissões a serem verificadas
     * @return {@code true} se o conjunto de permissões contiver todas as permissões fornecidas, caso contrário {@code false}
     */
    public boolean hasAllPermissions(List<@NotNull String> permissionList) {
        return permissionList.stream().allMatch(this::hasPermission);
    }
}
