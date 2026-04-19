package net.tutla.pvpPlus.commandSystem;


import net.tutla.pvpPlus.PvpPlus;
import net.tutla.pvpPlus.commandSystem.command.*;
import net.tutla.pvpPlus.arena.Arena;
import net.tutla.pvpPlus.kit.Kit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class CommandSystem {
    //private final CommandManhunt manhuntCommand = new CommandManhunt();
    PvpPlus pvpPlus;

    private final List<TutlaCommand> commands = new ArrayList<>();


    public void initialise() {
        //List<TutlaCommand> toPut = new ArrayList<>(manhuntCommand.getSubcommands());
        //toPut.addAll(commands);
        //CommandManhuntHelp.generateHelpString(toPut);
        pvpPlus = PvpPlus.getInstance();
        commands.add(new ArenaCommand(pvpPlus.getArenaManager()));
        commands.add(new KitCommand(pvpPlus.getKitManager()));
        commands.add(new PartyCommand(pvpPlus.getPartyManager()));
        commands.add(new DuelCommand(pvpPlus.getDuelManager(), pvpPlus.getFightManager(), pvpPlus.getKitManager()));
        commands.add(new QueueCommand(pvpPlus.getQueueManager(), pvpPlus.getKitManager()));
        commands.add(new LeaveFightCommand(pvpPlus.getFightManager()));
        commands.add(new SpectateCommand(pvpPlus.getFightManager()));
    }

    public boolean execute(CommandContext cmdParams){
        for (TutlaCommand command : commands){
            if (cmdParams.cmd.getName().equalsIgnoreCase(command.name())){
                if (!command.run(cmdParams)){
                    command.help(cmdParams.player);
                };
            }
        }
        return false;
    }

    public List<String> tabComplete(CommandContext ctx){
        if (ctx.args.length > 0) {
            for (TutlaCommand command : commands) {
                if (command.name().equalsIgnoreCase(ctx.cmd.getName())) {
                    int cidx = 0;
                    CommandTabAutoComplete complete = getAutoComplete(command.autocomplete, ctx, cidx);
                    if (complete != null) {
                        return generateAutoComplete(complete, ctx.args[ctx.args.length - 1]);
                    }
                    return Collections.emptyList();
                }
            }
        }
        return Collections.emptyList();
    }

    public CommandTabAutoComplete getAutoComplete(CommandTabAutoComplete complete, CommandContext ctx, int cidx) {
        if (cidx == ctx.args.length - 1) {
            return complete;
        }

        if (complete.childAutoCompletes != null) {
            for (CommandTabAutoComplete child : complete.childAutoCompletes) {
                if (child == null) continue;
                if (child.name.equalsIgnoreCase(ctx.args[cidx])) {
                    return getAutoComplete(child, ctx, cidx + 1);
                }
            }
        }

        if (complete.value != null && !complete.value.equals("<values>")) {
            return getAutoComplete(complete, ctx, cidx + 1);
        }

        return null;
    }

    public List<String> generateAutoComplete(CommandTabAutoComplete autocomplete, String arg){
        if (autocomplete.value == null || autocomplete.value.isBlank()){
            return Collections.emptyList();
        }

        switch (autocomplete.value) {
            case "<values>" -> {
                if (autocomplete.values == null) return Collections.emptyList();
                return autocomplete.values.stream()
                        .filter(s -> s.startsWith(arg.toLowerCase()))
                        .toList();
            }
            case "<player>" -> {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(arg.toLowerCase()))
                        .toList();
            }
            case "<arena>" -> {
                return pvpPlus.getArenaManager().getAllArenas().stream()
                        .map(Arena::getName)
                        .filter(name -> name.toLowerCase().startsWith(arg.toLowerCase()))
                        .toList();
            }
            case "<kit>" -> {
                return pvpPlus.getKitManager().getAllKits().stream()
                        .map(Kit::getName)
                        .filter(name -> name.toLowerCase().startsWith(arg.toLowerCase()))
                        .toList();
            }
            case "<leader>" -> {
                return pvpPlus.getPartyManager().getAllPartyLeaderNames().stream()
                        .filter(name -> name.toLowerCase().startsWith(arg.toLowerCase()))
                        .toList();
            }
            case "<subcommand|player>" -> {
                return Stream.concat(
                        Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(name -> name.toLowerCase().startsWith(arg.toLowerCase())),
                        autocomplete.values.stream()
                                .filter(s -> s.startsWith(arg.toLowerCase()))
                ).toList();
            }
            default -> {return Collections.emptyList();}
        }
    }
}