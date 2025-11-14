package net.warcane.lugin.core.punish.data;

import lombok.Getter;

/**
 * @author Rok, Pedro Lucas nmm. Created on 26/06/2025
 */
@Getter
public enum PunishmentStatus {

    ACTIVE("Ativa", 'a', "<l-green>"),
    REVOKED("Revogada", '7', "<l-gray>"),
    EXPIRED("Finalizada", 'c', "<l-red>");
    // TODO: Pendente status, fazer quando meialu voltar

    private final String title;
    private final char color;
    private final String modernColor;

    PunishmentStatus(final String title, final char color, String modernColor) {
        this.title = title;
        this.color = color;
        this.modernColor = modernColor;
    }

    public String getFormattedTitle() {
        return "§" + color + "(" + title + ")";
    }
}
