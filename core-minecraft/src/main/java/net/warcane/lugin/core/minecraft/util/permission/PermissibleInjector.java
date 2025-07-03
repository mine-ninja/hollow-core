package net.warcane.lugin.core.minecraft.util.permission;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Logger;

import static net.warcane.lugin.core.minecraft.util.permission.CraftBukkitInterface.craftClassName;


/**
 * This class handles injection of {@link Permissible}s into {@link Player}s for various server implementations.
 */
public abstract class PermissibleInjector {

    public static void injectPlayer(@NotNull Player player, @NotNull Permissible userPermissible) {
        for (PermissibleInjector injector : INJECTORS) {
            if (injector.isApplicable(player)) {
                try {
                    injector.inject(player, userPermissible);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void injectPlayer(@NotNull Player player) {
        for (PermissibleInjector injector : INJECTORS) {
            if (injector.isApplicable(player)) {
                try {
                    injector.inject(player, injector.getPermissible(player));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static final PermissibleInjector[] INJECTORS = {
      new ClassPresencePermissibleInjector(
        craftClassName("entity.CraftHumanEntity"),
        "perm",
        false
      )
    };

    protected final @Nullable String clazzName;
    protected final String fieldName;
    protected final boolean copyValues;

    PermissibleInjector(final @Nullable String clazzName, String fieldName, boolean copyValues) {
        this.clazzName = clazzName;
        this.fieldName = fieldName;
        this.copyValues = copyValues;
    }

    /**
     * Attempts to inject {@code permissible} into {@code player},
     *
     * @param player      The player to have {@code permissible} injected into
     * @param permissible The permissible to inject into {@code player}
     * @return the old permissible if the injection was successful, otherwise null
     * @throws NoSuchFieldException   when the permissions field could not be found in the Permissible
     * @throws IllegalAccessException when things go very wrong
     */
    public @Nullable Permissible inject(final Player player, final @Nullable Permissible permissible) throws NoSuchFieldException, IllegalAccessException {
        final @Nullable Field permField = getPermissibleField(player);
        if (permField == null) {
            return null;
        }
        Permissible oldPerm = (Permissible) permField.get(player);
        if (copyValues && permissible instanceof PermissibleBase newBase) {
            PermissibleBase oldBase = (PermissibleBase) oldPerm;
            copyValues(oldBase, newBase);
        }

        // Inject permissible
        permField.set(player, permissible);
        return oldPerm;
    }

    public Permissible getPermissible(final Player player) throws NoSuchFieldException, IllegalAccessException {
        return (Permissible) getPermissibleField(player).get(player);
    }

    private @Nullable Field getPermissibleField(final Player player) throws NoSuchFieldException {
        final Class<?> humanEntity;
        try {
            humanEntity = Class.forName(clazzName);
        } catch (ClassNotFoundException e) {
            Logger.getLogger("PermissionsEx").warning("[PermissionsEx] Unknown server implementation being used!");
            return null;
        }

        if (!humanEntity.isAssignableFrom(player.getClass())) {
            Logger.getLogger("PermissionsEx").warning("[PermissionsEx] Strange error while injecting permissible!");
            return null;
        }

        final Field permField = humanEntity.getDeclaredField(this.fieldName);
        // Make it public for reflection
        permField.setAccessible(true);
        return permField;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void copyValues(PermissibleBase old, PermissibleBase newPerm) throws NoSuchFieldException, IllegalAccessException {
        // Attachments
        Field attachmentField = PermissibleBase.class.getDeclaredField("attachments");
        attachmentField.setAccessible(true);
        List<Object> attachmentPerms = (List<Object>) attachmentField.get(newPerm);
        attachmentPerms.clear();
        attachmentPerms.addAll((List) attachmentField.get(old));
        newPerm.recalculatePermissions();
    }

    public abstract boolean isApplicable(Player player);

    static final class ClassPresencePermissibleInjector extends PermissibleInjector {
        ClassPresencePermissibleInjector(final @Nullable String clazzName, final String fieldName, final boolean copyValues) {
            super(clazzName, fieldName, copyValues);
        }

        @Override
        public boolean isApplicable(final Player player) {
            if (this.clazzName == null) {
                return false;
            }

            try {
                return Class.forName(this.clazzName).isInstance(player);
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}