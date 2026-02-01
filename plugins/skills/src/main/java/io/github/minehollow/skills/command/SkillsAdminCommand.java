package io.github.minehollow.skills.command;

import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.skills.SkillsPlugin;
import io.github.minehollow.skills.player.PlayerSkillsProgressController;
import io.github.minehollow.skills.skill.Skill;
import io.github.minehollow.skills.skill.SkillManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SkillsAdminCommand extends SimpleCommand {

    private final SkillsPlugin plugin;
    private final SkillManager skillManager;
    private final PlayerSkillsProgressController controller;

    public SkillsAdminCommand(@NotNull SkillsPlugin plugin) {
        super("skillsadmin", "skills.admin");
        this.plugin = plugin;
        this.skillManager = plugin.getSkillManager();
        this.controller = plugin.getPlayerSkillsProgressController();
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        if (ctx.getArgs().length == 0) {
            sendHelp(ctx);
            return;
        }

        String subCommand = ctx.getArgs()[0].toLowerCase();
        String[] args = Arrays.copyOfRange(ctx.getArgs(), 1, ctx.getArgs().length);

        switch (subCommand) {
            case "setlevel", "setl" -> setLevel(ctx.getSender(), args);
            case "setexp", "setexperience", "setxp" -> setExperience(ctx.getSender(), args);
            case "addlevel", "addl" -> addLevel(ctx.getSender(), args);
            case "addexp", "addexperience", "addxp" -> addExperience(ctx.getSender(), args);
            case "check", "info", "ver" -> check(ctx.getSender(), args);
            default -> throw new CommandFailedException("§cSubcomando desconhecido: " + subCommand);
        }
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        if (ctx.getArgs().length == 1) {
            return filterStartingWith(
              Arrays.asList("setlevel", "setexp", "addlevel", "addexp", "check"),
              ctx.getArgs()[0]
            );
        }

        if (ctx.getArgs().length >= 2) {
            String subCommand = ctx.getArgs()[0].toLowerCase();
            String[] args = Arrays.copyOfRange(ctx.getArgs(), 1, ctx.getArgs().length);

            switch (subCommand) {
                case "setlevel", "setl", "setexp", "setexperience", "setxp", "addlevel", "addl", "addexp",
                     "addexperience", "addxp", "check", "info", "ver" -> {
                    if (args.length == 1) {
                        return filterOnlinePlayers(args[0]);
                    } else if (args.length == 2) {
                        return filterSkills(args[1]);
                    }
                }
            }
        }

        return NONE_ARGS;
    }

    private void sendHelp(CommandContext ctx) {
        ctx.getSender().sendMessage("§e§lSkills Admin");
        ctx.getSender().sendMessage("§7Use os seguintes subcomandos:");
        ctx.getSender().sendMessage("§e/skillsadmin setlevel <jogador> <skill> <level>");
        ctx.getSender().sendMessage("§e/skillsadmin setexp <jogador> <skill> <experiência>");
        ctx.getSender().sendMessage("§e/skillsadmin addlevel <jogador> <skill> <level>");
        ctx.getSender().sendMessage("§e/skillsadmin addexp <jogador> <skill> <experiência>");
        ctx.getSender().sendMessage("§e/skillsadmin check <jogador> <skill>");
    }

    private void setLevel(CommandSender sender, String[] args) throws CommandFailedException {
        if (args.length < 3) {
            throw new CommandFailedException("§cUso: /skillsadmin setlevel <jogador> <skill> <level>");
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            throw new CommandFailedException("§cJogador não encontrado ou offline.");
        }

        String skillId = args[1];
        Skill skill = skillManager.getSkillById(skillId);
        if (skill == null) {
            throw new CommandFailedException("§cSkill '" + skillId + "' não existe.");
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            throw new CommandFailedException("§cO nível deve ser um número inteiro.");
        }

        if (level < 0) {
            throw new CommandFailedException("§cO nível não pode ser negativo.");
        }

        controller.addLevel(target, skillId, level - controller.getSkillLevel(target, skillId));
        sender.sendMessage("§aLevel da skill §e" + skill.getDisplayName() + "§a do jogador §e" +
                           target.getName() + "§a foi definido para §e" + level + "§a.");
    }

    private void setExperience(CommandSender sender, String[] args) throws CommandFailedException {
        if (args.length < 3) {
            throw new CommandFailedException("§cUso: /skillsadmin setexp <jogador> <skill> <experiência>");
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            throw new CommandFailedException("§cJogador não encontrado ou offline.");
        }

        String skillId = args[1];
        Skill skill = skillManager.getSkillById(skillId);
        if (skill == null) {
            throw new CommandFailedException("§cSkill '" + skillId + "' não existe.");
        }

        double experience;
        try {
            experience = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            throw new CommandFailedException("§cA experiência deve ser um número.");
        }

        if (experience < 0) {
            throw new CommandFailedException("§cA experiência não pode ser negativa.");
        }

        double currentExp = controller.getSkillExperience(target, skillId);
        controller.addSkillExperience(target, skillId, experience - currentExp);

        sender.sendMessage("§aExperiência da skill §e" + skill.getDisplayName() + "§a do jogador §e" +
                           target.getName() + "§a foi definida para §e" + experience + "§a.");
    }

    private void addLevel(CommandSender sender, String[] args) throws CommandFailedException {
        if (args.length < 3) {
            throw new CommandFailedException("§cUso: /skillsadmin addlevel <jogador> <skill> <level>");
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            throw new CommandFailedException("§cJogador não encontrado ou offline.");
        }

        String skillId = args[1];
        Skill skill = skillManager.getSkillById(skillId);
        if (skill == null) {
            throw new CommandFailedException("§cSkill '" + skillId + "' não existe.");
        }

        int levels;
        try {
            levels = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            throw new CommandFailedException("§cO nível deve ser um número inteiro.");
        }

        controller.addLevel(target, skillId, levels);
        int newLevel = controller.getSkillLevel(target, skillId);

        sender.sendMessage("§aAdicionado §e" + levels + "§a níveis à skill §e" +
                           skill.getDisplayName() + "§a do jogador §e" + target.getName() +
                           "§a. Nível atual: §e" + newLevel + "§a.");
    }

    private void addExperience(CommandSender sender, String[] args) throws CommandFailedException {
        if (args.length < 3) {
            throw new CommandFailedException("§cUso: /skillsadmin addexp <jogador> <skill> <experiência>");
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            throw new CommandFailedException("§cJogador não encontrado ou offline.");
        }

        String skillId = args[1];
        Skill skill = skillManager.getSkillById(skillId);
        if (skill == null) {
            throw new CommandFailedException("§cSkill '" + skillId + "' não existe.");
        }

        double experience;
        try {
            experience = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            throw new CommandFailedException("§cA experiência deve ser um número.");
        }

        controller.addSkillExperience(target, skillId, experience);
        double newExp = controller.getSkillExperience(target, skillId);

        sender.sendMessage("§aAdicionado §e" + experience + "§a de experiência à skill §e" +
                           skill.getDisplayName() + "§a do jogador §e" + target.getName() +
                           "§a. Experiência atual: §e" + String.format("%.2f", newExp) + "§a.");
    }

    private void check(CommandSender sender, String[] args) throws CommandFailedException {
        if (args.length < 2) {
            throw new CommandFailedException("§cUso: /skillsadmin check <jogador> <skill>");
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            throw new CommandFailedException("§cJogador não encontrado ou offline.");
        }

        String skillId = args[1];
        Skill skill = skillManager.getSkillById(skillId);
        if (skill == null) {
            throw new CommandFailedException("§cSkill '" + skillId + "' não existe.");
        }

        int level = controller.getSkillLevel(target, skillId);
        double experience = controller.getSkillExperience(target, skillId);

        sender.sendMessage("§e§lInformações da Skill");
        sender.sendMessage("§7Jogador: §f" + target.getName());
        sender.sendMessage("§7Skill: §f" + skill.getDisplayName() + " §8(" + skillId + ")");
        sender.sendMessage("§7Nível: §f" + level);
        sender.sendMessage("§7Experiência: §f" + String.format("%.2f", experience));
    }

    private List<String> filterOnlinePlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
          .collect(Collectors.toList());
    }

    private List<String> filterSkills(String prefix) {
        return skillManager.getAllSkills().stream()
          .map(Skill::getId)
          .filter(id -> id.toLowerCase().startsWith(prefix.toLowerCase()))
          .collect(Collectors.toList());
    }
}