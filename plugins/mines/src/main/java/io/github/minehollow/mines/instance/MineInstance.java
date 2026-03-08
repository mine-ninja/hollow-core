package io.github.minehollow.mines.instance;

import io.github.minehollow.mines.mine.MineDefinition;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

// Representa uma instancia de mina virtual para um jogador.
// cada jogador possui uma visão unica da mina, os blocos são totalmente
// client-side via packet, e a mina é gerada dinamicamente para cada jogador, permitindo
// que cada jogador tenha uma experiência única e personalizada ao explorar a mina.
// e também que a gente escale o número de jogadores na mina sem se preocupar com o desempenho do servidor.
@Getter
public final class MineInstance {

    private final UUID ownerId;
    private final MineDefinition definition;
    private final long seed;
    private volatile int currentLevel;
    private final AtomicLong resetEpoch;

    private final Set<UUID> members;
    private final LongOpenHashSet minedBlocks;

    public MineInstance(@NotNull UUID ownerId, @NotNull MineDefinition definition, int currentLevel, long seed) {
        this.ownerId = ownerId;
        this.definition = definition;
        this.currentLevel = currentLevel;
        this.seed = seed;
        this.resetEpoch = new AtomicLong(0L);
        this.members = ConcurrentHashMap.newKeySet();
        this.minedBlocks = new LongOpenHashSet();

        this.members.add(ownerId);
    }

    public synchronized boolean addMember(@NotNull UUID uuid) {
        return this.members.add(uuid);
    }

    public synchronized boolean removeMember(@NotNull UUID uuid) {
        if (uuid.equals(ownerId)) {
            return false;
        }

        return this.members.remove(uuid);
    }

    public boolean hasMember(@NotNull UUID uuid) {
        return this.members.contains(uuid);
    }

    public synchronized boolean markMined(int x, int y, int z) {
        return this.minedBlocks.add(packBlockPos(x, y, z));
    }

    public synchronized boolean isMined(int x, int y, int z) {
        return this.minedBlocks.contains(packBlockPos(x, y, z));
    }

    public synchronized void reset() {
        this.minedBlocks.clear();
        this.resetEpoch.incrementAndGet();
    }

    public long currentEpoch() {
        return this.resetEpoch.get();
    }

    public static long packBlockPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
            | ((long) (z & 0x3FFFFFF) << 12)
            | (y & 0xFFFL);
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }
}
