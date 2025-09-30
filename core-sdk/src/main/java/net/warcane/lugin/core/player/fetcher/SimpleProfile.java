package net.warcane.lugin.core.player.fetcher;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@Data @AllArgsConstructor
public class SimpleProfile {
    private UUID uuid;
    private String name;
    private String skin;
}
