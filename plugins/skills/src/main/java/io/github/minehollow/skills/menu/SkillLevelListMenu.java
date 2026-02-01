package io.github.minehollow.skills.menu;

import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.menu.pagination.DynamicPaginationContext;
import io.github.minehollow.minecraft.menu.pagination.DynamicPaginationMenu;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.RomanConverter;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import io.github.minehollow.skills.SkillsPlugin;
import io.github.minehollow.skills.skill.Skill;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class SkillLevelListMenu extends DynamicPaginationMenu<Integer> {

    private static final ItemStack PREVIOUS_PAGE_ICON = ItemBuilder.of(Material.ARROW)
      .name("<yellow>Pagina Anterior")
      .build();

    private static final ItemStack NEXT_PAGE_ICON = ItemBuilder.of(Material.ARROW)
      .name("<yellow>Próxima Página")
      .build();

    private static final ItemStack BACK_TO_MENU_ICON = ItemBuilder.of(Material.CRIMSON_DOOR)
      .name("<red>Voltar ao Menu de Habilidades")
      .removeAllFlags()
      .addLore(
        "",
        "<gray>Clique para voltar ao menu",
        "<gray>principal de habilidades.",
        ""
      )
      .build();

    private static final IntArrayList LEVELS = IntArrayList.wrap(
      IntStream.range(1, Skill.MAX_SKILL_LEVEL).toArray()
    );

    private final SkillsPlugin plugin;

    @Override
    public boolean onPreOpen(
      @NotNull DynamicPaginationContext<Integer> ctx,
      @NotNull MenuConfig openHandler
    ) {
        final var skill = Objects.requireNonNull(ctx.<Skill>get("skill"), "Skill cannot be null in SkillLevelListMenu");

        openHandler.setTitle(StringUtils.text(skill.getDisplayName()));
        openHandler.setRows(6);
        openHandler.setLayout(
          "         ",
          "  LLLLL  ",
          "P LLLLL N",
          "  LLLLL  ",
          "    M    "
        );


        Tasks.runAsync(() -> ctx.setPagination(
          'L',
          LEVELS,
          (player, level) -> this.generateSkillLevelIcon(player, skill, level)
        ));

        ctx.setNextButton('N', NEXT_PAGE_ICON);
        ctx.setPreviousButton('P', PREVIOUS_PAGE_ICON);
        ctx.setItem('M', BACK_TO_MENU_ICON, event -> ctx.openMenu(SkillsMainMenu.class));

        openHandler.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5F, 1.0F));
        return true;
    }


    private @NotNull ItemStack generateSkillLevelIcon(
      @NotNull Player player,
      @NotNull Skill skill,
      int level
    ) {
        final int currentPlayerLevel = skill.getCurrentLevel(player);

        final Material material;
        final String color;
        if (level > currentPlayerLevel) {
            material = Material.GREEN_STAINED_GLASS_PANE;
            color = "<green>";
        } else if (level == currentPlayerLevel) {
            material = Material.YELLOW_STAINED_GLASS_PANE;
            color = "<yellow>";
        } else {
            material = Material.GREEN_STAINED_GLASS_PANE;
            color = "<green>";
        }

        return ItemBuilder.of(material)
          .name(color + "Nível " + RomanConverter.toRoman(level) + "<dark_gray> (" + level + ")")
          .removeAllEnchant()
          .addLore(
            "",
            "<green><bold>Recompensas: ",
            ""
          )
          .lore(skill.getRewardDescription(level)
            .stream()
            .map(desc -> "<gray>○ <white>" + desc)
            .toList()
          )
          .build();
    }
}