package net.warcane.lugin.core.player.wallet;

import com.mongodb.client.model.Indexes;
import net.warcane.lugin.core.util.data.MongoRepository;
import net.warcane.lugin.core.util.data.RedisCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class PlayerWalletService {

    private final Map<UUID, PlayerWallet> localCachedWallets = new ConcurrentHashMap<>();
    private final RedisCache<PlayerWallet> redisCachedWallet = new RedisCache<>(PlayerWallet.class);
    private final MongoRepository<UUID, PlayerWallet> walletRepository = new MongoRepository<>(PlayerWallet.class, "uniqueId");

    private final ExecutorService executorService;

    public PlayerWalletService(@NotNull ExecutorService executorService) {
        this.executorService = executorService;
        walletRepository.useCollection(collection -> collection.createIndex(Indexes.hashed("uniqueId")));
    }

    @Nullable
    public PlayerWallet getCachedWallet(@NotNull UUID playerId) {
        return localCachedWallets.get(playerId);
    }
}
