package net.warcane.lugin.core.network.packet.impl.generic;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;
import net.warcane.lugin.core.util.JsonUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Representa um pacote de rede genérico que utiliza JSON para serialização e desserialização.
 * Este pacote pode ser usado para enviar dados estruturados entre o cliente e o servidor
 * de forma flexível e extensível.
 *
 * @param content O conteúdo do pacote, que é uma string JSON representando os dados a serem transmitidos.
 */
public record JsonPacket(@JsonProperty("c") String content) implements NetworkPacket {

    /**
     * Cria um novo pacote JSON a partir de um objeto genérico.
     *
     * @param content O conteúdo do pacote, que será convertido para uma string JSON.
     * @param <T>     O tipo do conteúdo do pacote.
     * @return Um novo JsonPacket contendo o conteúdo serializado como JSON.
     */
    public static <T> JsonPacket createPacket(@NotNull T content) {
        return new JsonPacket(JsonUtil.toJson(content));
    }

    /**
     * Obtem o conteúdo do pacote como uma string JSON.
     *
     * @param type O tipo de classe para o qual o conteúdo deve ser convertido.
     * @param <T>  O tipo de classe para o qual o conteúdo deve ser convertido.
     * @return O conteúdo do pacote convertido para o tipo especificado.
     */
    public <T> T getContentAs(Class<T> type) {
        return JsonUtil.fromJson(content, type);
    }


    /**
     * Obtem o conteúdo do pacote como uma lista de objetos do tipo especificado.
     *
     * @param type O tipo de classe para os objetos na lista.
     * @param <T>  O tipo de classe para os objetos na lista.
     * @return Uma lista de objetos do tipo especificado, convertidos a partir do conteúdo JSON.
     */
    public <T> List<T> getContentAsList(Class<T> type) {
        return JsonUtil.fromJsonList(content, type);
    }
}
