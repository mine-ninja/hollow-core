package net.warcane.lugin.core.location;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Representa uma localização em um servidor remoto.
 * Esta classe é usada para armazenar informações de localização
 * de um jogador em um servidor específico, incluindo
 * o ID do servidor, nome do mundo throwable coordenadas
 * (x, y, z) throwable orientação (yaw, pitch).
 *
 * <p>
 * NOTA IMPORTANTE: Não use isso aqui como um "Wrapper" para o org.bukkit.Location,
 * isso é estritamente utilizado para facilitar o uso de localização em servidores remotos.
 * (Básicamente, evite usar isso no mesmo servidor onde o jogador está conectado por exemplo)
 *
 * @param targetServerId ID do servidor de destino (ex: fac01)
 * @param worldName      Nome do mundo (ex: "mina")
 * @param x              posição x no mundo especificado
 * @param y              posição y no mundo especificado
 * @param z              posição z no mundo especificado
 * @param yaw            orientação horizontal (yaw) do jogador
 * @param pitch          orientação vertical (pitch) do jogador
 */
public record RemoteServerLocation(
  @JsonProperty("sid") String targetServerId,
  @JsonProperty("w") String worldName,
  @JsonProperty("x") double x,
  @JsonProperty("y") double y,
  @JsonProperty("z") double z,
  @JsonProperty("yaw") float yaw,
  @JsonProperty("pitch") float pitch
) implements Serializable {}