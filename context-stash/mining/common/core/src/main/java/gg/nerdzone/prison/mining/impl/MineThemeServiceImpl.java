/*
 * Copyright (c) 2024-2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.impl;

import gg.nerdzone.prison.mining.model.theme.MineTheme;
import gg.nerdzone.prison.mining.services.MiningThemeService;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default implementation of the mining theme service.
 *
 * @see MiningThemeService
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MineThemeServiceImpl implements MiningThemeService {

    public static @NotNull MineThemeServiceImpl create() {
        return new MineThemeServiceImpl();
    }

    @Override
    public @NonNull Optional<MineTheme> findTheme(@Nullable String themeId) {
        return Optional.ofNullable(MineTheme.findByName(themeId)).or(() -> Optional.of(MineTheme.defaultTheme()));
    }
}
