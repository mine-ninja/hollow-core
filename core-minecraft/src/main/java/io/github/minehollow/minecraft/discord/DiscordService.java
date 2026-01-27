package io.github.minehollow.minecraft.discord;

import io.github.minehollow.sdk.database.RedisConnector;
import io.github.minehollow.minecraft.BukkitPlatform;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DiscordService {

    private static final String DISCORD_CODE_KEY = "discord_link_code:";
    private static final String DISCORD_UUID_KEY = "discord_link_uuid:";

    @NotNull
    private final BukkitPlatform platform;

    @NotNull
    private final RedisConnector redisConnector;

    public DiscordService(@NotNull BukkitPlatform platform) {
        this.platform = platform;
        this.redisConnector = RedisConnector.getInstance();
    }

    public String createNewCode(UUID playerId) {
        var code = DiscordCodeGenerator.generateCode();

        return redisConnector.supplyFromJedis(jedis -> {
            var existingCode = jedis.get(DISCORD_UUID_KEY + playerId);
            if (existingCode != null) {
                return existingCode;
            }

            var transaction = jedis.multi();

            transaction.setex(DISCORD_CODE_KEY + code, 300, playerId.toString());
            transaction.setex(DISCORD_UUID_KEY + playerId, 300, code);
            transaction.exec();

            return code;
        });
    }
}
