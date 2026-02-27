package io.github.minehollow.clans.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Permission nodes stored per clan member.
 */
@Getter
@RequiredArgsConstructor
public enum ClanPermission {

    CHAT("Chat do Clan", "Enviar mensagens no chat do clan"),
    MANAGE_MEMBERS("Gerenciar Membros", "Convidar, expulsar e promover membros"),
    UPGRADES("Melhorias", "Gastar fundos do clan na loja de melhorias"),
    PVP_CONTROL("Controle de PvP", "Ativar/desativar fogo amigo");

    private final String displayName;
    private final String description;

    public static @Nullable ClanPermission fromName(@NotNull String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

