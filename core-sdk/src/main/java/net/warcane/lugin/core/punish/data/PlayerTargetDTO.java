package net.warcane.lugin.core.punish.data;

import java.util.UUID;

/**
 * @author Rok, Pedro Lucas nmm. Created on 30/06/2025
 * @project punish
 */
public record PlayerTargetDTO(String name, UUID uuid, boolean isReport) {
}
