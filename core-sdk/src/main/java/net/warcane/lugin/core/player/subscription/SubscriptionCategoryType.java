package net.warcane.lugin.core.player.subscription;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Representa categorias de assinaturas disponíveis no servidor.
 * As categorias são usadas para organizar diferentes tipos de assinaturas que os jogadores podem adquirir.
 * Por ex: Você pode ter assinaturas para toda a network ou somente para o servidor de Factions.
 */
@Getter
@AllArgsConstructor
public enum SubscriptionCategoryType {

    GLOBAL("Global"),
    FACTIONS("Factions"),
    MINIGAMES("Minigames");

    private final String displayName;
}
