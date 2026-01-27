package io.github.minehollow.skills.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SkillType {

    MINING("mining", "Mining"),
    WOODCUTTING("woodcutting", "Woodcutting"),
    FARMING("farming", "Farming"),
    FISHING("fishing", "Fishing");


    private final String id;
    private final String displayName;
}
