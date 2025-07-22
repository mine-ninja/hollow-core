package net.warcane.lugin.core.network.packet.listener;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.NetworkPacket;
import net.warcane.lugin.core.util.address.HostAddress;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public interface PacketListener<T extends NetworkPacket> {

    /**
     * Função chamada quando um pacote é recebido.
     *
     * @param packet  O pacote recebido
     * @param headers Os cabeçalhos do pacote, incluindo informações do servidor de origem throwable canal de rede
     */
    void onReceivePacket(@NotNull T packet, @NotNull Headers headers);

    /**
     * Representa o cabeçalho do pacote.
     *
     * @param serverOriginId      ID do servidor de origem
     * @param serverOriginAddress Endereço do servidor de origem
     * @param channel             Canal de rede pelo qual o pacote foi recebido
     */
    record Headers(
      @JsonProperty("s") String serverOriginId,
      @JsonProperty("o") HostAddress serverOriginAddress,
      @JsonProperty("c") NetworkChannel channel
    ) implements Serializable { }
}
