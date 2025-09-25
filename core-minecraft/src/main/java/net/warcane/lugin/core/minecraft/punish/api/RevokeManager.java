package net.warcane.lugin.core.minecraft.punish.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import net.warcane.lugin.core.minecraft.util.message.ComponentBuilder;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import net.warcane.lugin.core.punish.data.PunishedDTO;
import net.warcane.lugin.core.punish.data.PunishmentStatus;
import net.warcane.lugin.core.punish.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * @author Rok, Pedro Lucas nmm. Created on 01/07/2025
 * @project punish
 */
public class RevokeManager {

    private static RevokeManager instance;

    public void startRevokeSession(Player player, int id, @Nullable RevokeAction action) {

        if (id == -1) {
            player.sendMessage("§cVocê precisa fornecer um ID de punição válido.");
            return;
        }
        PunishManager.get().getPunishmentById(id).whenComplete((punish, throwable) -> {
            if (throwable != null) {
                player.sendMessage("§cErro ao buscar punição: " + throwable.getMessage());
                return;
            }
            if (punish.getStatus().equals(PunishmentStatus.REVOKED)) {
                player.sendMessage("§cA punição de ID #" + id + " já foi revogada.");
                return;
            }
            if (!checkPunishCanBeRevoked(player, punish)) return;
            if (action == null) {
                manageRevokeSession(player, punish);
                return;
            }
            applyRevoke(player, punish, action);
        });
    }

    private void manageRevokeSession(Player player, PunishedDTO.Punishment punishment) {
        Audience audience = BukkitPlatformPlugin.getInstance().adventure().player(player);
        ComponentBuilder msg = ComponentBuilder.of();
        msg.newLine().newLine();
        msg.simple("<l-yellow>Selecione um motivo:");
        msg.newLine().newLine();

        for (RevokeAction value : RevokeAction.values()) {
            if (value == RevokeAction.NONE) continue;
            if (!player.hasPermission(value.permission)) {
                continue;
            }
            msg.simple(" <l-gray>• ");
            msg.suggestHover("<l-white>" + value.displayName + " ",
                    "/revogar " + punishment.getId() + " " + value.name() + " ",
                    "<l-yellow>" + value.displayName, "<l-white>" + value.lore,
                    "",
                "<l-white>Grupo mínimo: <l-green>" + MessageUtils.getFormatedPermission(value.permission));
            msg.newLine();
        }
        msg.newLine();
        msg.actionHover("  <l-red><b>CANCELAR", (audience1 -> {
            StringUtils.send(audience, "\n\n\n<l-info>Ação cancelada com sucesso!\n");
        }), "<l-gray>Clique para cancelar.");
        msg.send(audience);
    }

    private void applyRevoke(Player player, PunishedDTO.Punishment punishment, RevokeAction action) {
        Audience audience = BukkitPlatformPlugin.getInstance().adventure().player(player);
        UUID uniqueId = player.getUniqueId();

        punishment.setRevokerUuid(uniqueId);
        punishment.setRevokedAt(System.currentTimeMillis());
        punishment.setRevokeReason(action.displayName);
        punishment.setStatus(PunishmentStatus.REVOKED);

        PunishManager.get().updatePunishmentStatus(punishment.getId(), punishment);

        PunishManager.get().getPunishLogger().logRevoke(punishment, player.getName());

        StringUtils.send(audience,"<l-confirm>Punição revogada com sucesso!");
    }


    private boolean checkPunishCanBeRevoked(Player player, PunishedDTO.Punishment punishment) {
        Audience audience = BukkitPlatformPlugin.getInstance().adventure().player(player);
        final long timeWhenApplied = punishment.getAppliedAt();
        // 3 hours to revoke
        final boolean revokeExpiredTime = System.currentTimeMillis() - timeWhenApplied > (3 * 60 * 60 * 1000);
        final boolean isAdmin = player.hasPermission("lugin.admin");
        final boolean isSelfPunish = punishment.getPunisherUuid().equals(player.getUniqueId());

        if (punishment.getRevokedAt() != 0) {
            StringUtils.send(audience,"<l-error>Essa punição já foi revogada.");
            return false;
        }
        if (revokeExpiredTime && !isAdmin) {
            StringUtils.send(audience,"<l-error>O prazo de revogar essa punição expirou. Entre em contato com um Administrador para que ele possa revogar a punição.");
            return false;
        }
        if (!isSelfPunish && !isAdmin) {
            StringUtils.send(audience,"<l-error>Você não tem permissão para revogar essa punição.");
            return false;
        }
        return true;
    }

    public static RevokeManager get() {
        if (instance == null) {
            instance = new RevokeManager();
        }
        return instance;
    }

    @Getter
    @RequiredArgsConstructor
    public enum RevokeAction {
        INCORRECT_PLAYER("Jogador incorreto", "Utilize quando a punição for aplicada ao jogador incorreto.", "lugin.helper"),
        INCORRECT_MOTIVE("Motivo incorreto", "Utilize quando o motivo da punição estiver incorreto.", "lugin.helper"),
        INCORRECT_EVIDENCE("Prova incorreta", "Utilize quando a prova anexada estiver incorreta.", "lugin.helper"),
        WRONG_APPLY("Punição aplicada incorretamente", "Utilize quando a punição for aplicada incorretamente.", "lugin.helper"),
        DUPLICATED("Punição duplicada", "Utilize quando já houver uma punição ativa por esse mesmo motivo.", "lugin.helper"),
        ACCEPTED("Revisão aceita", "Após uma análise da supervisão, foi constatado que a punição foi aplicada indevidamente.", "lugin.admin"),
        NONE("Indefinido", "Nenhum motivo selecionado.", "");

        private final String displayName;
        private final String lore;
        private final String permission;

        public static RevokeAction fromString(String action) {
            if (action == null) return NONE;
            for (RevokeAction revokeAction : values()) {
                if (revokeAction.name().equalsIgnoreCase(action)) {
                    return revokeAction;
                }
            }
            return NONE;
        }
    }
}
