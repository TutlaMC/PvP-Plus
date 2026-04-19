package net.tutla.pvpPlus.commandSystem.command;

import net.tutla.pvpPlus.commandSystem.*;
import net.tutla.pvpPlus.manager.ArenaManager;
import net.tutla.pvpPlus.arena.Arena;
import net.tutla.pvpPlus.util.TextUtil;
import org.bukkit.Location;

import java.util.List;

public class ArenaCommand extends TutlaCommand {

    private final ArenaManager arenaManager;

    public ArenaCommand(ArenaManager arenaManager) {
        super("arena", "/arena <subcommand>", "Manage PvP arenas",
                CommandSection.SETUP,
                new CommandTabAutoComplete("arena",
                        List.of(
                                new CommandTabAutoComplete("create",   List.of(), ""),
                                new CommandTabAutoComplete("setpos1",  List.of(), ""),
                                new CommandTabAutoComplete("setpos2",  List.of(), ""),
                                new CommandTabAutoComplete("addspawn1",List.of(), ""),
                                new CommandTabAutoComplete("addspawn2",List.of(), ""),
                                new CommandTabAutoComplete("save",     List.of(), ""),
                                new CommandTabAutoComplete("cancel",   List.of(), ""),
                                new CommandTabAutoComplete("delete",   List.of(), "<arena>"),
                                new CommandTabAutoComplete("list",     List.of(), ""),
                                new CommandTabAutoComplete("restore",  List.of(), "<arena>")
                        ),
                        "<values>"
                ).setValues(List.of(
                        "create","setpos1","setpos2",
                        "addspawn1","addspawn2",
                        "save","cancel","delete","list", "restore"
                ))
        );
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean run(CommandContext ctx) {
        if (ctx.args.length == 0) return false;

        String sub = ctx.args[0].toLowerCase();
        switch (sub) {
            case "create"    -> runCreate(ctx);
            case "setpos1"   -> runSetPos(ctx, 1);
            case "setpos2"   -> runSetPos(ctx, 2);
            case "addspawn1" -> runAddSpawn(ctx, 1);
            case "addspawn2" -> runAddSpawn(ctx, 2);
            case "save"      -> runSave(ctx);
            case "cancel"    -> runCancel(ctx);
            case "delete"    -> runDelete(ctx);
            case "list"      -> runList(ctx);
            case "restore"   -> runRestore(ctx);
            default          -> { return false; }
        }
        return true;
    }

    // /arena create <name>
    private void runCreate(CommandContext ctx) {
        if (ctx.args.length < 2) {
            ctx.player.sendMessage(TextUtil.parse("<red>Usage: /arena create <name>"));
            return;
        }
        String name = ctx.args[1];
        if (arenaManager.isInSetup(ctx.player)) {
            ctx.player.sendMessage(TextUtil.parse(
                    "<red>You're already setting up an arena. Use <yellow>/arena save</yellow> or <yellow>/arena cancel</yellow> first."));
            return;
        }
        if (!arenaManager.startSetup(ctx.player, name)) {
            ctx.player.sendMessage(TextUtil.parse("<red>An arena named <yellow>" + name + "</yellow> already exists."));
            return;
        }
        ctx.player.sendMessage(TextUtil.parse(
                "<green>Arena <yellow>" + name + "</yellow> setup started.</green>\n" +
                        "<gray>Steps:\n" +
                        "  1. Stand at corner 1 → <white>/arena setpos1</white>\n" +
                        "  2. Stand at corner 2 → <white>/arena setpos2</white>\n" +
                        "  3. Stand at a team 1 spawn → <white>/arena addspawn1</white> (repeat for more)\n" +
                        "  4. Stand at a team 2 spawn → <white>/arena addspawn2</white> (repeat for more)\n" +
                        "  5. <white>/arena save</white> when done."));
    }

    // /arena setpos1  |  /arena setpos2
    private void runSetPos(CommandContext ctx, int pos) {
        if (!arenaManager.isInSetup(ctx.player)) {
            ctx.player.sendMessage(TextUtil.parse("<red>Start an arena setup first with <yellow>/arena create <name></yellow>."));
            return;
        }
        Arena arena = arenaManager.getSetupArena(ctx.player);
        if (pos == 1) arena.setPos1(ctx.player.getLocation());
        else          arena.setPos2(ctx.player.getLocation());
        ctx.player.sendMessage(TextUtil.parse(
                "<green>Position " + pos + " set to <yellow>"
                        + formatLoc(ctx.player.getLocation()) + "</yellow>."));
    }

    // /arena addspawn1  |  /arena addspawn2
    private void runAddSpawn(CommandContext ctx, int team) {
        if (!arenaManager.isInSetup(ctx.player)) {
            ctx.player.sendMessage(TextUtil.parse("<red>Start an arena setup first with <yellow>/arena create <name></yellow>."));
            return;
        }
        Arena arena = arenaManager.getSetupArena(ctx.player);
        if (team == 1) arena.addTeam1Spawn(ctx.player.getLocation());
        else           arena.addTeam2Spawn(ctx.player.getLocation());
        int count = (team == 1 ? arena.getTeam1Spawns() : arena.getTeam2Spawns()).size();
        ctx.player.sendMessage(TextUtil.parse(
                "<green>Team " + team + " spawn #" + count + " added at <yellow>"
                        + formatLoc(ctx.player.getLocation()) + "</yellow>."));
    }

    // /arena save
    private void runSave(CommandContext ctx) {
        if (!arenaManager.isInSetup(ctx.player)) {
            ctx.player.sendMessage(TextUtil.parse("<red>No arena setup in progress."));
            return;
        }
        Arena arena = arenaManager.getSetupArena(ctx.player);

        if (arena.getPos1() == null || arena.getPos2() == null) {
            ctx.player.sendMessage(TextUtil.parse("<red>Set both corner positions first (<yellow>/arena setpos1</yellow> and <yellow>/arena setpos2</yellow>)."));
            return;
        }
        if (arena.getTeam1Spawns().isEmpty()) {
            ctx.player.sendMessage(TextUtil.parse("<red>Add at least one team 1 spawn with <yellow>/arena addspawn1</yellow>."));
            return;
        }
        if (arena.getTeam2Spawns().isEmpty()) {
            ctx.player.sendMessage(TextUtil.parse("<red>Add at least one team 2 spawn with <yellow>/arena addspawn2</yellow>."));
            return;
        }

        arenaManager.saveSetup(ctx.player);
        ctx.player.sendMessage(TextUtil.parse(
                "<green>Arena <yellow>" + arena.getName() + "</yellow> saved! " +
                        "<gray>(" + arena.getSnapshotSize() + " blocks captured)"));
    }

    private void runCancel(CommandContext ctx) {
        if (!arenaManager.isInSetup(ctx.player)) {
            ctx.player.sendMessage(TextUtil.parse("<red>No arena setup in progress."));
            return;
        }
        String name = arenaManager.getSetupArena(ctx.player).getName();
        arenaManager.cancelSetup(ctx.player);
        ctx.player.sendMessage(TextUtil.parse("<yellow>Arena setup for <white>" + name + "</white> cancelled."));
    }

    // /arena delete <name>
    private void runDelete(CommandContext ctx) {
        if (ctx.args.length < 2) {
            ctx.player.sendMessage(TextUtil.parse("<red>Usage: /arena delete <name>"));
            return;
        }
        String name = ctx.args[1];
        if (!arenaManager.deleteArena(name)) {
            ctx.player.sendMessage(TextUtil.parse("<red>No arena named <yellow>" + name + "</yellow> found."));
            return;
        }
        ctx.player.sendMessage(TextUtil.parse("<green>Arena <yellow>" + name + "</yellow> deleted."));
    }

    // /arena list
    private void runList(CommandContext ctx) {
        var arenas = arenaManager.getAllArenas();
        if (arenas.isEmpty()) {
            ctx.player.sendMessage(TextUtil.parse("<yellow>No arenas configured yet."));
            return;
        }
        ctx.player.sendMessage(TextUtil.parse("<green>--- Arenas ---"));
        for (Arena a : arenas) {
            String status = a.isInUse() ? "<red>[IN USE]" : "<green>[FREE]";
            String config = a.isFullyConfigured() ? "<green>✔" : "<red>✘ incomplete";
            ctx.player.sendMessage(TextUtil.parse(
                    status + " </> <white>" + a.getName() +
                            "</white> <gray>| spawns: T1=" + a.getTeam1Spawns().size() +
                            " T2=" + a.getTeam2Spawns().size() +
                            " | " + config));
        }
    }

    // /arena restore
    private void runRestore(CommandContext ctx){
        if (ctx.args.length < 2) {
            ctx.player.sendMessage(TextUtil.parse("<red>Usage: /arena restore <name>"));
            return;
        }
        String name = ctx.args[1];
        Arena arena = arenaManager.getArena(name);
        if (arena == null) {
            ctx.player.sendMessage(TextUtil.parse("<red>No arena named <yellow>" + name + "</yellow> found."));
            return;
        }

        arena.restoreSnapshot();
        arena.setInUse(false);

        ctx.player.sendMessage(TextUtil.parse("<green>Arena <yellow>" + name + "</yellow> restored."));
    }


    // util
    private String formatLoc(Location l) {
        return String.format("%.1f, %.1f, %.1f", l.getX(), l.getY(), l.getZ());
    }
}
