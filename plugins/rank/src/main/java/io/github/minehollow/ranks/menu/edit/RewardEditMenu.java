package io.github.minehollow.ranks.menu.edit;

import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.message.input.ChatInput;
import io.github.minehollow.minecraft.util.range.IntRange;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import io.github.minehollow.ranks.RanksPlugin;
import io.github.minehollow.ranks.reward.RankReward;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RewardEditMenu extends SimpleMenu {
    private final RanksPlugin plugin;

    public RewardEditMenu(RanksPlugin plugin) {
        this.plugin = plugin;
        this.defaultConfig.setGlobalClickCancelled(false);
    }

    // Método auxiliar para garantir que sempre temos uma String pura
    private static String ensureString(Object obj) {
        if (obj == null) return "";
        return String.valueOf(obj).trim();
    }

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig config) {
        Player player = ctx.getPlayer();

        if (!player.hasPermission("ranks.admin")) {
            player.sendMessage(StringUtils.text("<red>Você não tem permissão para editar recompensas!"));
            return false;
        }

        RankReward reward = ctx.get("reward");
        boolean isNew = reward == null;

        if (isNew) {
            String rewardId = ensureString(ctx.get("rewardId"));
            if (rewardId.isEmpty()) {
                player.sendMessage(StringUtils.text("<red>ID da recompensa não especificado!"));
                return false;
            }

            // Criar nova recompensa com valores padrão
            reward = new RankReward(
              rewardId,
              IntRange.parseString("1"),
              0,
              null,
              rewardId,  // displayName inicial = ID
              List.of(),
              List.of()
            );
            ctx.put("reward", reward);
            ctx.put("tempCommands", new ArrayList<String>());
            ctx.put("tempItems", new ArrayList<ItemStack>());
        } else {
            // Carregar dados existentes em variáveis temporárias
            if (ctx.get("tempCommands") == null) {
                ctx.put("tempCommands", new ArrayList<>(reward.commandsToExecute()));
            }
            if (ctx.get("tempItems") == null) {
                ctx.put("tempItems", new ArrayList<>(reward.itemsToGive()));
            }
        }

        final RankReward currentReward = reward;

        // Construir título de forma segura
        String titlePrefix = isNew ? "Criando Recompensa: " : "Editando Recompensa: ";
        String displayName = ensureString(currentReward.displayName());
        config.setTitle(StringUtils.text(titlePrefix + displayName));

        config.setRows(6);
        config.setLayout(
          "IIIIIIIII",
          "IIIIIIIII",
          "IIIIIIIII",
          "IIIIIIIII",
          "---------",
          "NREPXC-SD");

        config.setGlobalClickCancelled(false);
        config.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5f, 1f));

        // SEPARATOR
        ctx.setItem('-', this::createSeparator, e -> e.setCancelled(true));

        // NAME
        ctx.setItem('N', p -> createNameButton(currentReward), e -> {
            e.setCancelled(true);
            saveItemsFromInventory(ctx);
            openNameInput(ctx);
        });

        // RANGE
        ctx.setItem('R', p -> createRangeButton(currentReward), e -> {
            e.setCancelled(true);
            saveItemsFromInventory(ctx);
            openRangeInput(ctx);
        });

        // EVERY X LEVELS
        ctx.setItem('E', p -> createEveryXLevelsButton(currentReward), e -> {
            e.setCancelled(true);
            saveItemsFromInventory(ctx);
            openEveryXLevelsInput(ctx);
        });

        // PERMISSION
        ctx.setItem('P', p -> createPermissionButton(currentReward), e -> {
            e.setCancelled(true);
            saveItemsFromInventory(ctx);
            openPermissionInput(ctx);
        });

        // COMMANDS
        ctx.setItem('X', p -> createCommandsButton(ctx), e -> {
            e.setCancelled(true);
            saveItemsFromInventory(ctx);
            openCommandsInput(ctx);
        });

        // COMMAND LIST
        ctx.setItem('C', p -> createCommandListButton(ctx), e -> {
            e.setCancelled(true);
            player.sendMessage(StringUtils.text("<yellow>Comandos atuais:"));
            List<String> commands = ctx.get("tempCommands");
            if (commands == null || commands.isEmpty()) {
                player.sendMessage(StringUtils.text("<gray>Nenhum comando configurado."));
            } else {
                for (int i = 0; i < commands.size(); i++) {
                    player.sendMessage(StringUtils.text("<gray>" + (i + 1) + ". <white>" + commands.get(i)));
                }
            }
        });

        // SAVE
        ctx.setItem('S', this::createSaveButton, e -> {
            e.setCancelled(true);
            saveReward(ctx);
        });

        // DELETE
        if (!isNew) {
            ctx.setItem('D', this::createDeleteButton, e -> {
                e.setCancelled(true);
                deleteReward(ctx, currentReward);
            });
        }

        return true;
    }

    @Override
    public void onPostOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig config) {
        RankReward reward = ctx.get("reward");
        if (reward != null) {
            loadRewardItems(ctx, reward);
        }
    }

    @Override
    protected void onClick(@NotNull PlayerMenuContext ctx, @NotNull InventoryClickEvent event) {
        int slot = event.getRawSlot();
        int[] itemSlots = ctx.getMenuConfig().getLayout().get('I');

        boolean isItemSlot = false;
        for (int s : itemSlots) {
            if (slot == s) {
                isItemSlot = true;
                break;
            }
        }

        if (isItemSlot || slot >= ctx.getInventory().getSize()) {
            return;
        }

        event.setCancelled(true);
    }

    private void loadRewardItems(PlayerMenuContext ctx, RankReward reward) {
        List<ItemStack> items = ctx.get("tempItems");
        if (items == null) {
            items = new ArrayList<>(reward.itemsToGive());
            ctx.put("tempItems", items);
        }

        int[] slots = ctx.getMenuConfig().getLayout().get('I');
        for (int i = 0; i < slots.length && i < items.size(); i++) {
            if (items.get(i) != null) {
                ctx.getInventory().setItem(slots[i], items.get(i).clone());
            }
        }
    }

    private void saveItemsFromInventory(PlayerMenuContext ctx) {
        int[] slots = ctx.getMenuConfig().getLayout().get('I');
        List<ItemStack> items = new ArrayList<>();
        for (int slot : slots) {
            ItemStack item = ctx.getInventory().getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }
        ctx.put("tempItems", items);
    }

    // ===== INPUT HANDLERS =====

    private void openNameInput(PlayerMenuContext ctx) {
        RankReward reward = ctx.get("reward");
        if (reward == null) return;

        Player player = ctx.getPlayer();
        player.closeInventory();

        ChatInput.waitInput(player, input -> {
            String inputStr = ensureString(input);

            if (!inputStr.equalsIgnoreCase("cancelar") && !inputStr.isEmpty()) {
                RankReward updated = new RankReward(
                  reward.id(),
                  reward.range(),
                  reward.everyXLevels(),
                  reward.permissionToReceive(),
                  inputStr,
                  reward.commandsToExecute(),
                  reward.itemsToGive()
                );
                ctx.put("reward", updated);
                player.sendMessage(StringUtils.text("<green>Nome alterado para: " + inputStr));
            } else {
                player.sendMessage(StringUtils.text("<red>Edição cancelada."));
            }
            Tasks.runSync(() -> ctx.openMenu(RewardEditMenu.class, true));
        }, 30000L, "<green>Digite o novo nome da recompensa no chat.\n\n\n<gray>Digite <red>cancelar <gray>para voltar.");
    }

    private void openRangeInput(PlayerMenuContext ctx) {
        RankReward reward = ctx.get("reward");
        if (reward == null) return;

        Player player = ctx.getPlayer();
        player.closeInventory();

        ChatInput.waitInput(player, input -> {
            String inputStr = ensureString(input);

            if (!inputStr.equalsIgnoreCase("cancelar") && !inputStr.isEmpty()) {
                try {
                    IntRange range = IntRange.parseString(inputStr);
                    RankReward updated = new RankReward(
                      reward.id(),
                      range,
                      reward.everyXLevels(),
                      reward.permissionToReceive(),
                      reward.displayName(),
                      reward.commandsToExecute(),
                      reward.itemsToGive()
                    );
                    ctx.put("reward", updated);
                    player.sendMessage(StringUtils.text("<green>Intervalo alterado para: " + range));
                } catch (Exception e) {
                    player.sendMessage(StringUtils.text("<red>Formato inválido! Use: 1-10 ou 5. Alteração cancelada."));
                }
            } else {
                player.sendMessage(StringUtils.text("<red>Edição cancelada."));
            }
            Tasks.runSync(() -> ctx.openMenu(RewardEditMenu.class, true));
        }, 30000L, "<green>Digite o intervalo de níveis (Ex: 1-10, 5-20, 15).\n\n\n<gray>Digite <red>cancelar <gray>para voltar.");
    }

    private void openEveryXLevelsInput(PlayerMenuContext ctx) {
        RankReward reward = ctx.get("reward");
        if (reward == null) return;

        Player player = ctx.getPlayer();
        player.closeInventory();

        ChatInput.waitInput(player, input -> {
            String inputStr = ensureString(input);

            if (!inputStr.equalsIgnoreCase("cancelar") && !inputStr.isEmpty()) {
                try {
                    int everyX = Integer.parseInt(inputStr);
                    RankReward updated = new RankReward(
                      reward.id(),
                      reward.range(),
                      everyX,
                      reward.permissionToReceive(),
                      reward.displayName(),
                      reward.commandsToExecute(),
                      reward.itemsToGive()
                    );
                    ctx.put("reward", updated);
                    player.sendMessage(StringUtils.text("<green>Frequência alterada para: a cada " + everyX + " níveis"));
                } catch (NumberFormatException e) {
                    player.sendMessage(StringUtils.text("<red>Número inválido! Alteração cancelada."));
                }
            } else {
                player.sendMessage(StringUtils.text("<red>Edição cancelada."));
            }
            Tasks.runSync(() -> ctx.openMenu(RewardEditMenu.class, true));
        }, 30000L, "<green>Digite a cada quantos níveis dar a recompensa (Ex: 5, 10).\n<gray>Use 0 para dar em todos os níveis do intervalo.\n\n<gray>Digite <red>cancelar <gray>para voltar.");
    }

    private void openPermissionInput(PlayerMenuContext ctx) {
        RankReward reward = ctx.get("reward");
        if (reward == null) return;

        Player player = ctx.getPlayer();
        player.closeInventory();

        ChatInput.waitInput(player, input -> {
            String inputStr = ensureString(input);

            if (!inputStr.equalsIgnoreCase("cancelar")) {
                String permission = inputStr.isEmpty() || inputStr.equalsIgnoreCase("nenhuma") ? null : inputStr;
                RankReward updated = new RankReward(
                  reward.id(),
                  reward.range(),
                  reward.everyXLevels(),
                  permission,
                  reward.displayName(),
                  reward.commandsToExecute(),
                  reward.itemsToGive()
                );
                ctx.put("reward", updated);
                player.sendMessage(StringUtils.text("<green>Permissão alterada para: " + (permission == null ? "nenhuma" : permission)));
            } else {
                player.sendMessage(StringUtils.text("<red>Edição cancelada."));
            }
            Tasks.runSync(() -> ctx.openMenu(RewardEditMenu.class, true));
        }, 30000L, "<green>Digite a permissão necessária para receber.\n<gray>Use <yellow>nenhuma <gray>para remover.\n\n<gray>Digite <red>cancelar <gray>para voltar.");
    }

    private void openCommandsInput(PlayerMenuContext ctx) {
        Player player = ctx.getPlayer();
        Tasks.runSync(player::closeInventory);
        List<String> commands = ctx.get("tempCommands");
        if (commands == null) {
            commands = new ArrayList<>();
            ctx.put("tempCommands", commands);
        }

        final List<String> currentCommands = commands;

        ChatInput.waitInput(player, input -> {
            String inputStr = ensureString(input);

            if (!inputStr.equalsIgnoreCase("cancelar") && !inputStr.isEmpty()) {
                if (inputStr.equalsIgnoreCase("limpar")) {
                    currentCommands.clear();
                    player.sendMessage(StringUtils.text("<green>Todos os comandos foram removidos."));
                } else {
                    currentCommands.add(inputStr);
                    player.sendMessage(StringUtils.text("<green>Comando adicionado: <white>" + inputStr));
                    player.sendMessage(StringUtils.text("<yellow>Digite outro comando ou <red>cancelar <yellow>para finalizar."));

                    ctx.openMenu(RewardEditMenu.class, true);
                    return;
                }
            } else {
                player.sendMessage(StringUtils.text("<gray>Finalizando edição de comandos."));
            }
            Tasks.runSync(() -> ctx.openMenu(RewardEditMenu.class, true));
        }, 60000L, "<green>Digite um comando para adicionar (use %player% para o jogador).\n<gray>Digite <yellow>limpar <gray>para remover todos.\n<gray>Digite <red>cancelar <gray>para finalizar.\n\n<yellow>Comandos atuais: " + currentCommands.size());
    }

    // ===== SAVE & DELETE =====

    private void saveReward(PlayerMenuContext ctx) {
        Player player = ctx.getPlayer();
        RankReward reward = ctx.get("reward");
        if (reward == null) return;

        saveItemsFromInventory(ctx);

        List<String> commands = ctx.getOrDefault("tempCommands", new ArrayList<>());
        List<ItemStack> items = ctx.getOrDefault("tempItems", new ArrayList<>());

        // Garantir que o displayName é uma String pura
        String safeDisplayName = ensureString(reward.displayName());

        RankReward finalReward = new RankReward(
          reward.id(),
          reward.range(),
          reward.everyXLevels(),
          reward.permissionToReceive(),
          safeDisplayName,
          commands != null ? List.copyOf(commands) : List.of(),
          items != null ? List.copyOf(items) : List.of()
        );

        plugin.getRankRewardManager().saveReward(finalReward);

        player.closeInventory();
        player.sendMessage(StringUtils.text("<green>Recompensa <white>" + safeDisplayName + " <green>salva com sucesso!"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }

    private void deleteReward(PlayerMenuContext ctx, RankReward reward) {
        Player player = ctx.getPlayer();

        plugin.getRankRewardManager().deleteReward(reward.id());

        player.closeInventory();
        player.sendMessage(StringUtils.text("<green>Recompensa <white>" + reward.displayName() + " <green>deletada!"));
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1f);
    }

    // ===== ITEM BUILDERS =====

    private ItemStack createSeparator(Player player) {
        return ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
          .name(" ")
          .build();
    }

    private ItemStack createNameButton(RankReward reward) {
        return ItemBuilder.of(Material.NAME_TAG)
          .name("<white>Editar Nome")
          .lore(
            "<gray>Nome atual: <white>" + ensureString(reward.displayName()),
            "",
            "<dark_gray>Clique para editar"
          )
          .build();
    }

    private ItemStack createRangeButton(RankReward reward) {
        return ItemBuilder.of(Material.COMPASS)
          .name("<white>Editar Intervalo")
          .lore(
            "<gray>Intervalo atual: <yellow>" + reward.range(),
            "",
            "<dark_gray>Clique para editar"
          )
          .build();
    }

    private ItemStack createEveryXLevelsButton(RankReward reward) {
        String frequency = reward.everyXLevels() == 0
          ? "Todos os níveis"
          : "A cada " + reward.everyXLevels() + " níveis";

        return ItemBuilder.of(Material.REPEATER)
          .name("<white>Editar Frequência")
          .lore(
            "<gray>Frequência atual: <yellow>" + frequency,
            "",
            "<dark_gray>Clique para editar"
          )
          .build();
    }

    private ItemStack createPermissionButton(RankReward reward) {
        String perm = reward.permissionToReceive() != null
          ? reward.permissionToReceive()
          : "Nenhuma";

        return ItemBuilder.of(Material.TRIPWIRE_HOOK)
          .name("<white>Editar Permissão")
          .lore(
            "<gray>Permissão atual: <yellow>" + perm,
            "",
            "<dark_gray>Clique para editar"
          )
          .build();
    }

    private ItemStack createCommandsButton(PlayerMenuContext ctx) {
        List<String> commands = ctx.get("tempCommands");
        int count = commands != null ? commands.size() : 0;

        return ItemBuilder.of(Material.COMMAND_BLOCK)
          .name("<white>Adicionar Comandos")
          .lore(
            "<gray>Comandos configurados: <yellow>" + count,
            "",
            "<dark_gray>Clique para adicionar/editar"
          )
          .build();
    }

    private ItemStack createCommandListButton(PlayerMenuContext ctx) {
        List<String> commands = ctx.get("tempCommands");
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Comandos configurados:");
        lore.add("");

        if (commands == null || commands.isEmpty()) {
            lore.add("<gray>Nenhum comando");
        } else {
            for (int i = 0; i < Math.min(commands.size(), 5); i++) {
                lore.add("<yellow>" + (i + 1) + ". <white>" + commands.get(i));
            }
            if (commands.size() > 5) {
                lore.add("<gray>... e mais " + (commands.size() - 5));
            }
        }
        lore.add("");
        lore.add("<dark_gray>Clique para ver todos");

        return ItemBuilder.of(Material.WRITABLE_BOOK)
          .name("<white>Lista de Comandos")
          .lore(lore)
          .build();
    }

    private ItemStack createSaveButton(Player player) {
        return ItemBuilder.of(Material.EMERALD)
          .name("<green>Salvar Recompensa")
          .lore("<dark_gray>Clique para salvar")
          .build();
    }

    private ItemStack createDeleteButton(Player player) {
        return ItemBuilder.of(Material.BARRIER)
          .name("<red>Deletar Recompensa")
          .lore("<dark_gray>Clique para deletar")
          .build();
    }
}