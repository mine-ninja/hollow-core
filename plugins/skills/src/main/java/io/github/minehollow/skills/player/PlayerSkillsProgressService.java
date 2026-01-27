package io.github.minehollow.skills.player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSkillsProgressService {

    private final Map<UUID, PlayerSkillsProgress> playerSkillsProgressMap;

    public PlayerSkillsProgressService() {
        this.playerSkillsProgressMap = new ConcurrentHashMap<>();
    }
}
