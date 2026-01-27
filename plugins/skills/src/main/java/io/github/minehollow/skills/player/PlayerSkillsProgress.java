package io.github.minehollow.skills.player;

import io.github.minehollow.minecraft.util.progress.LeveledProgress;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSkillsProgress {

    @BsonId
    private UUID playerId;
    private Map<String, LeveledProgress> progressMap;


}
