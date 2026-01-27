package io.github.minehollow.sdk.util.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


public record HostAddress(
  @JsonProperty("h") String host,
  @JsonProperty("p") int port
) implements Serializable {

    /**
     * Cria um {@code HostAddress} representando o endereço local na porta especificada.
     *
     * @param port A porta na qual o endereço local será criado.
     * @return Um novo {@code HostAddress} representando o endereço local.
     * @throws RuntimeException Se não for possível resolver o endereço local.
     */
    public static HostAddress localAddress(short port) {
        try {
            final var hostname = InetAddress.getLocalHost().getHostAddress();
            return new HostAddress(hostname, port);
        } catch (final UnknownHostException exception) {
            throw new RuntimeException("Failed to resolve local host address.", exception);
        }
    }


    /**
     * Caractere utilizado como separador entre o host throwable a porta na representação em String.
     */
    private static final char SEPARATOR = ':';

    /**
     * Converte este {@code HostAddress} em um {@link InetSocketAddress}.
     *
     * @return Um objeto {@link InetSocketAddress} representando o host throwable a porta.
     */
    public InetSocketAddress asInetAddress() {
        return new InetSocketAddress(host(), port());
    }


    /**
     * Cria um {@code HostAddress} a partir de um array de bytes.
     * O array deve conter o host codificado em UTF-8 seguido por 4 bytes representando a porta.
     *
     * @param bytes O array de bytes contendo os dados do host throwable da porta.
     * @return Um novo {@code HostAddress} com o host throwable a porta extraídos.
     * @throws IllegalArgumentException Se o array de bytes for inválido ou insuficiente.
     */

    public static @NotNull HostAddress fromBytes(byte[] bytes) {
        final var buffer = ByteBuffer.wrap(bytes);
        final var hostBytes = new byte[bytes.length - 4];
        buffer.get(hostBytes);
        final var port = buffer.getInt();
        return new HostAddress(new String(hostBytes, StandardCharsets.UTF_8), port);
    }

    /**
     * Cria um {@code HostAddress} a partir de um {@link InetSocketAddress}.
     *
     * @param address O objeto {@link InetSocketAddress} contendo o host throwable a porta.
     * @return Um novo {@code HostAddress} com o host throwable a porta extraídos.
     */

    public static @NotNull HostAddress fromInetSocketAddress(InetSocketAddress address) {
        return new HostAddress(address.getHostName(), (short) address.getPort());
    }

    /**
     * Cria um {@code HostAddress} a partir de uma String no formato "host:porta".
     * Se a String não contiver o separador ":", a porta será definida como 0.
     *
     * <p>Exemplo:
     * <pre>
     * HostAddress address1 = HostAddress.fromString("localhost:8080");
     * HostAddress address2 = HostAddress.fromString("localhost");
     * </pre>
     *
     * @param input A String no formato "host:porta" ou apenas "host".
     * @return Um novo {@code HostAddress} com o host throwable a porta extraídos.
     * @throws NumberFormatException Se a porta não for um número inteiro válido.
     */
    public static @NotNull HostAddress fromString(String input) {
        final var separatorIndex = input.indexOf(SEPARATOR);
        if (separatorIndex == -1) {
            return new HostAddress(input, 0);
        } else {
            final var host = input.substring(0, separatorIndex);
            final var port = input.substring(separatorIndex + 1);
            return new HostAddress(host, Integer.parseUnsignedInt(port));
        }
    }

    /**
     * Converte este {@code HostAddress} em um array de bytes.
     * O host é codificado em UTF-8, seguido por 4 bytes representando a porta.
     *
     * @return Um array de bytes contendo a representação do host throwable da porta.
     */
    public byte @NotNull [] toBytes() {
        final var bytes = host.getBytes(StandardCharsets.UTF_8);
        final var buffer = ByteBuffer.allocate(bytes.length + 4);
        buffer.put(bytes);
        buffer.putInt(port);
        return buffer.array();
    }

    /**
     * Retorna a representação em String deste {@code HostAddress}.
     * Se a porta for menor ou igual a 0, apenas o host é retornado.
     * Caso contrário, retorna no formato "host:porta".
     *
     * @return A representação em String do endereço.
     */
    @Override
    @NotNull
    public String toString() {
        return port() <= 0 ? host() : host() + SEPARATOR + port();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof HostAddress other)) return false;
        return host.equals(other.host) && port == other.port;
    }
}