package io.github.minehollow.sdk.player.permissions;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import static java.util.Comparator.comparingInt;

/**
 * Represents the permission dictionary.
 * in this plugin permissions work hierarchically.
 * <p>
 * For example:
 * "*" means all permissions.
 * "{permission}.*" means all permissions that starts with {permission}.
 * "{permission}.example" means the permission {permission}.example.
 * <p>
 * this class is used to store the permission in a data structure.
 *
 * @author Matheus Barreto
 * @since 1.0.0
 */
public class PermissionGraph {

    private static final Comparator<WeightedPermissionNode> WEIGHT_COMPARATOR = comparingInt(WeightedPermissionNode::getWeight);


    private static final String HIGHEST_PERMISSION = "*";
    private static final Splitter SPLITTER = Splitter.on(".");
    private final Set<WeightedPermissionNode> permissions = new ConcurrentSkipListSet<>(WEIGHT_COMPARATOR);


    private static final PermissionGraph INSTANCE = new PermissionGraph();

    public static PermissionGraph getInstance() {
        return INSTANCE;
    }


    public Iterable<String> getHigherPermissions(@NotNull String permission) {
        WeightedPermissionNode permissionNode = this.getPermissionNode(permission);
        if (permissionNode == null) {
            return Collections.emptySet();
        }

        return permissionNode.getSubPermissions();
    }

    /**
     * Add all possible higher permissions to the graph.
     * Example: ["permission.sub.inner"] should register
     * "permission.sub.inner" , "permission.sub.*" and "permission.*"
     *
     * @param permission the permission to generate the sub permissions.
     * @return the generated sub permissions.
     */
    public Set<String> generateSubPermissionsFor(@NotNull String permission) {
        Iterable<String> split = SPLITTER.split(permission);

        int iterableSize = Iterables.size(split);
        if (iterableSize <= 0) {
            return Collections.emptySet();
        }

        Set<String> validSubPermissions = new TreeSet<>();

        StringBuilder nodePerm = new StringBuilder();
        for (String subParts : split) {
            nodePerm.append(subParts).append(".");
            validSubPermissions.add(nodePerm + "*");
        }

        return validSubPermissions;
    }


    /**
     * Registers a new sub subPermission to the graph.
     * <p>
     * Example: ["permission.sub.inner"] should register
     * "permission.sub.inner" , "permission.sub.*" and "permission.*"
     * </p>
     *
     * @param subPermission the subPermission to register.
     */
    public void addPermission(@NotNull String subPermission) {
        subPermission = subPermission.toLowerCase();

        // Empty subPermission means that the permission is the highest possible.
        // or an invalid permission.
        Set<String> subPermissions = this.generateSubPermissionsFor(subPermission);
        if (subPermissions.isEmpty()) {
            return;
        }

        String highestPossibleNode = this.findHighestPermissionNode(subPermission);

        WeightedPermissionNode permissionNode = this.getPermissionNode(highestPossibleNode);
        if (permissionNode == null) {
            permissionNode = new WeightedPermissionNode(highestPossibleNode, new HashSet<>());
            permissions.add(permissionNode);
        }

        permissionNode.getSubPermissions().addAll(subPermissions);
    }

    /**
     * Searchs for the highest permission node.
     * Example: ["permission.sub.inner"] should return "permission.*"
     *
     * @param subPermission the subPermission to search.
     * @return the highest permission node.
     */

    public String findHighestPermissionNode(@NotNull String subPermission) {
        WeightedPermissionNode fastSearchNode = this.getPermissionNode(subPermission);
        if (fastSearchNode != null) {
            return fastSearchNode.getPermission();
        }


        if (subPermission.equalsIgnoreCase(HIGHEST_PERMISSION)) {
            return HIGHEST_PERMISSION;
        }

        Iterable<String> split = SPLITTER.split(subPermission);

        int iterableSize = Iterables.size(split);
        if (iterableSize <= 0) {
            return HIGHEST_PERMISSION;
        }

        String firstPart = Iterables.get(split, 0);
        String highestPossibleNode = firstPart + ".*";

        WeightedPermissionNode permissionNode = this.getPermissionNode(highestPossibleNode);
        if (permissionNode == null) {
            permissionNode = new WeightedPermissionNode(highestPossibleNode, new HashSet<>());
            permissions.add(permissionNode);
        }

        return permissionNode.getPermission();
    }


    private WeightedPermissionNode getPermissionNode(@NotNull String permission) {
        for (WeightedPermissionNode permissionNode : permissions) {
            if (permissionNode.containsPermission(permission)) {
                return permissionNode;
            }
        }
        return null;
    }


    @Data
    static class WeightedPermissionNode {
        private final String permission;
        private final Set<String> subPermissions;

        public boolean containsPermission(@NotNull String permission) {
            if (this.permission.equals(permission)) {
                return true;
            }

            return subPermissions.contains(permission.toLowerCase());
        }

        public int getWeight() {
            return subPermissions.size();
        }
    }
}
