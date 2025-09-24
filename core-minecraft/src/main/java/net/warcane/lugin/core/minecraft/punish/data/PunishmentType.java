package net.warcane.lugin.core.minecraft.punish.data;

import lombok.Getter;

/**
 * @author Rok, Pedro Lucas nmm. Created on 26/06/2025
 * @project punish
 */
@Getter
public enum PunishmentType {

    TEMP("Banimento"),
    MUTE("Silenciamento"),
    PERM("Banimento");

    private final String title;

    PunishmentType(final String title) {
        this.title = title;
    }
}
