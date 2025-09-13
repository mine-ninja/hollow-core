package net.warcane.lugin.core.server.type;

import org.jetbrains.annotations.ApiStatus;

public enum ServerSubCategoryType {
    /**
     * Usado internamente para representar a ausência de uma subcategoria.
     * Não deve ser usado em lógica, pois qualquer servidor sem subcategoria definida
     * será tratado como NONE.
     */
    @ApiStatus.Internal
    NONE,
    
    MINA_1,
    MINA_2,
    MINA_3,
    MINA_4;
}
