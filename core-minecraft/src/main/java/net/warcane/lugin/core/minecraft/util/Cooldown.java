package net.warcane.lugin.core.minecraft.util;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A classe Cooldown oferece um sistema simples de gerenciamento de cooldown para jogadores.
 * Os cooldowns são associados a uma chave única e podem ser definidos em milissegundos ou segundos.
 * Cada jogador pode ter múltiplos cooldowns associados a chaves diferentes.
 *
 * @author Lucasmellof, Lucas de Mello Freitas created on 18/11/2021
 */
public class Cooldown {

    private static final Set<CooldownInstance> cooldownInstanceSet =  ConcurrentHashMap.newKeySet();

    /**
     * Obtém uma instância de cooldown para um jogador específico e uma chave única.
     *
     * @param uuid O UUID do jogador.
     * @param key  A chave única associada ao cooldown.
     *
     * @return Uma instância de CooldownInstance ou null se não houver cooldown associado à chave e ao jogador.
     */
    public static CooldownInstance getCooldown(UUID uuid, String key) {
        for(CooldownInstance cooldownInstance : cooldownInstanceSet) {
            if (uuid.equals(cooldownInstance.uuid()) && cooldownInstance.key().equals(key)) {
                return cooldownInstance;
            }
        }
        return null;
    }

    /**
     * Remove um cooldown associado a um jogador e uma chave única.
     *
     * @param uuid O UUID do jogador.
     * @param key  A chave única associada ao cooldown.
     */
    public static void removeCooldown(UUID uuid, String key) {
        cooldownInstanceSet.removeIf(cooldownInstance -> uuid.equals(cooldownInstance.uuid()) && cooldownInstance.key().equals(key));
    }

    /**
     * Remove all Coolodwn
     */
    public static void removeAllCooldown() {
        cooldownInstanceSet.clear();
    }

    /**
     * Verifica se um jogador está em cooldown para uma chave única.
     *
     * @param uuid O UUID do jogador.
     * @param key  A chave única associada ao cooldown.
     *
     * @return True se o jogador estiver em cooldown, False caso contrário.
     */
    public static boolean isInCooldown(UUID uuid, String key) {
        CooldownInstance cooldownInstance;
        if ((cooldownInstance = getCooldown(uuid, key)) != null) {
            if (cooldownInstance.time() > System.currentTimeMillis()) {
                return true;
            } else {
                removeCooldown(uuid, key);
                return false;
            }
        }
        return false;
    }

    /**
     * Retorna true e define o cooldown se o jogador não estiver em cooldown para a chave única.
     *
     * @param uuid O UUID do jogador.
     * @param time O tempo de cooldown em milissegundos.
     * @param key  A chave única associada ao cooldown.
     */
    public static boolean setIfNotInCooldown(UUID uuid, Long time, String key) {
        if (!isInCooldown(uuid, key)) {
            setCooldownMili(uuid, time, key);
            return true;
        }
        return false;
    }

    /**
     * Retorna true e define o cooldown se o jogador não estiver em cooldown para a chave única.
     *
     * @param uuid O UUID do jogador.
     * @param time O tempo de cooldown em segundos.
     * @param key  A chave única associada ao cooldown.
     */
    public static boolean setIfNotInCooldownSec(UUID uuid, Long time, String key) {
        if (!isInCooldown(uuid, key)) {
            setCooldownSec(uuid, time, key);
            return true;
        }
        return false;
    }

    /**
     * Define um cooldown em milissegundos para um jogador e uma chave única.
     *
     * @param uuid O UUID do jogador.
     * @param time O tempo de cooldown em milissegundos.
     * @param key  A chave única associada ao cooldown.
     */
    public static void setCooldownMili(UUID uuid, Long time, String key) {
        cooldownInstanceSet.add(new CooldownInstance(uuid, System.currentTimeMillis() + (time), key));
    }

    /**
     * Define um cooldown em segundos para um jogador e uma chave única.
     *
     * @param uuid O UUID do jogador.
     * @param time O tempo de cooldown em segundos.
     * @param key  A chave única associada ao cooldown.
     */
    public static void setCooldownSec(UUID uuid, Long time, String key) {
        cooldownInstanceSet.add(new CooldownInstance(uuid, System.currentTimeMillis() + (time * 1000), key));
    }

    /**
     * Obtém o tempo restante de cooldown em milissegundos para um jogador e uma chave única.
     *
     * @param uuid O UUID do jogador.
     * @param key  A chave única associada ao cooldown.
     *
     * @return O tempo restante de cooldown em milissegundos.
     */
    public static long getCooldownTime(UUID uuid, String key) {
        CooldownInstance cooldownInstance;
        if ((cooldownInstance = getCooldown(uuid, key)) != null) {
            long time = cooldownInstance.time() - System.currentTimeMillis();
            if (time < 0) {
                removeCooldown(uuid, key);
                return 0;
            }
            return time;
        }
        return 0;
    }

    /**
     * Obtém o tempo restante de cooldown em segundos para um jogador e uma chave única.
     *
     * @param uuid O UUID do jogador.
     * @param key  A chave única associada ao cooldown.
     *
     * @return O tempo restante de cooldown em segundos.
     */
    public static long getCooldownTimeSec(UUID uuid, String key) {
        return getCooldownTime(uuid, key) / 1000;
    }

    /**
     * Obtém o tempo restante de cooldown em milissegundos para um jogador e uma chave única.
     *
     * @param uuid O UUID do jogador.
     * @param key  A chave única associada ao cooldown.
     *
     * @return O tempo restante de cooldown em milissegundos.
     */
    public static long getCooldownTimeMili(UUID uuid, String key) {
        return getCooldownTime(uuid, key) % 1000;
    }

    /**
     * Classe interna que representa uma instância de cooldown associada a um jogador, tempo e chave única.
     *
     * @param uuid Obtém o UUID do jogador associado a esta instância de cooldown.
     * @param time Obtém o tempo associado a esta instância de cooldown.
     * @param key  Obtém a chave única associada a esta instância de cooldown.
     */
    public record CooldownInstance(UUID uuid, Long time, String key) { }
}
