package net.warcane.lugin.core.player.subscription;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * Representa categorias de assinaturas disponíveis no servidor.
 * As categorias são usadas para organizar diferentes tipos de assinaturas que os jogadores podem adquirir.
 * Por ex: Você pode ter assinaturas para toda a network ou somente para o servidor de Factions.
 */
@Getter
@AllArgsConstructor
public enum SubscriptionCategoryType {

    GLOBAL("Global");

    public static final Map<String, SubscriptionCategoryType> BY_NAME = Arrays.stream(values())
      .collect(toMap(SubscriptionCategoryType::getDisplayName, Function.identity()));


    private final String displayName;
}
