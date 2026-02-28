package io.github.minehollow.clans.hook;

import io.github.minehollow.clans.model.Clan;
import io.github.minehollow.clans.service.ClanService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for the Clans plugin.
 * <p>
 * Placeholders:
 * <ul>
 *   <li>{@code %clans_tag%}         — Clan tag (e.g. "ABC")</li>
 *   <li>{@code %clans_name%}        — Clan name</li>
 *   <li>{@code %clans_has_clan%}    — "true" / "false"</li>
 *   <li>{@code %clans_members%}     — Current member count</li>
 *   <li>{@code %clans_max_members%} — Max members for the clan's tier</li>
 *   <li>{@code %clans_role%}        — "Líder" or "Membro"</li>
 *   <li>{@code %clans_owner%}       — Owner's name</li>
 * </ul>
 */
public class PapiHook extends PlaceholderExpansion {

    private final ClanService clanService;
    private final int[] slotTable;

    public PapiHook(@NotNull ClanService clanService, int[] slotTable) {
        this.clanService = clanService;
        this.slotTable = slotTable;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "clans";
    }

    @Override
    public @NotNull String getAuthor() {
        return "sasuked";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) {
            return null;
        }

        Clan clan = clanService.getByPlayer(offlinePlayer.getUniqueId());

        return switch (params.toLowerCase()) {
            case "has_clan" -> clan != null ? "true" : "false";
            case "tag" -> clan != null ? " [" + clan.getTag() + "]" : "";
            case "name" -> clan != null ? clan.getName() : "";
            case "members" -> clan != null ? String.valueOf(clan.getMembers().size()) : "0";
            case "max_members" -> clan != null ? String.valueOf(clan.getMaxMembers(slotTable)) : "0";
            case "role" -> {
                if (clan == null) {
                    yield "";
                }
                yield clan.isOwner(offlinePlayer.getUniqueId()) ? "Líder" : "Membro";
            }
            case "owner" -> {
                if (clan == null) {
                    yield "";
                }
                OfflinePlayer owner = Bukkit.getOfflinePlayer(clan.getOwnerId());
                yield owner.getName() != null ? owner.getName() : "???";
            }
            default -> null;
        };
    }
}

