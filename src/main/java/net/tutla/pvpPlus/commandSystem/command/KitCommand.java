package net.tutla.pvpPlus.commandSystem.command;

import net.tutla.pvpPlus.commandSystem.*;
import net.tutla.pvpPlus.kit.Kit;
import net.tutla.pvpPlus.kit.KitManager;
import net.tutla.pvpPlus.util.TextUtil;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class KitCommand extends TutlaCommand {

    private final KitManager kitManager;

    public KitCommand(KitManager kitManager) {
        super("kit", "/kit <subcommand>", "Manage PvP kits",
                CommandSection.SETUP,
                new CommandTabAutoComplete("kit",
                        List.of(
                                new CommandTabAutoComplete("create",  List.of(), "<name>"),
                                new CommandTabAutoComplete("save", List.of(), "<kit>"),
                                new CommandTabAutoComplete("load",    List.of(), "<kit>"),
                                new CommandTabAutoComplete("delete",  List.of(), "<kit>"),
                                new CommandTabAutoComplete("list",    List.of(), ""),
                                new CommandTabAutoComplete("config",  List.of(), "<kit>"),
                                new CommandTabAutoComplete("icon",  List.of(), "<kit>"),
                                new CommandTabAutoComplete("rounds", List.of(), "")
                        ),
                        "<values>"
                ).setValues(List.of("create", "save", "load", "delete", "list", "config", "icon", "rounds"))
        );
        this.kitManager = kitManager;
    }

    @Override
    public boolean run(CommandContext ctx) {
        if (ctx.args.length == 0) return false;
        switch (ctx.args[0].toLowerCase()) {
            case "create"  -> runCreate(ctx);
            case "save"    -> runSave(ctx);
            case "load"    -> runLoad(ctx);
            case "delete"  -> runDelete(ctx);
            case "list"    -> runList(ctx);
            case "config"  -> runConfig(ctx);
            case "icon" -> runIcon(ctx);
            case "rounds" -> runRounds(ctx);
            default        -> { return false; }
        }
        return true;
    }


    // /kit create <name>
    // Puts the admin into a session and switches them to creative
    private void runCreate(CommandContext ctx) {
        if (ctx.args.length < 2) {
            ctx.player.sendMessage(TextUtil.parse("<red>Usage: /kit create <name>"));
            return;
        }
        if (!kitManager.createKit(ctx.args[1])) {
            ctx.player.sendMessage(TextUtil.parse(
                    "<red>A kit named <yellow>" + ctx.args[1] + "</yellow> already exists."));
            return;
        }
        ctx.player.sendMessage(TextUtil.parse(
                "<green>Kit <yellow>" + ctx.args[1] + "</yellow> created.\n" +
                        "<gray>Fill your inventory then run <white>/kit save " + ctx.args[1] + "</white>."));
    }

    private void runSave(CommandContext ctx) {
        if (ctx.args.length < 2) {
            ctx.player.sendMessage(TextUtil.parse("<red>Usage: /kit save <name>"));
            return;
        }
        if (!kitManager.saveKit(ctx.player, ctx.args[1])) {
            ctx.player.sendMessage(TextUtil.parse(
                    "<red>No kit named <yellow>" + ctx.args[1] + "</yellow> found. " +
                            "Create it first with <yellow>/kit create " + ctx.args[1] + "</yellow>."));
            return;
        }
        ctx.player.sendMessage(TextUtil.parse(
                "<green>Kit <yellow>" + ctx.args[1] + "</yellow> saved!"));
    }

    // /kit load <name>
    // Shows the admin the kit's contents in chat and optionally applies it
    private void runLoad(CommandContext ctx) {
        if (ctx.args.length < 2) {
            ctx.player.sendMessage(TextUtil.parse("<red>Usage: /kit load <name>"));
            return;
        }
        Kit kit = kitManager.getKit(ctx.args[1]);
        if (kit == null) {
            ctx.player.sendMessage(TextUtil.parse(
                    "<red>No kit named <yellow>" + ctx.args[1] + "</yellow> found."));
            return;
        }
        // Apply the kit directly to the admin so they can inspect it
        kitManager.applyKit(ctx.player, kit);
        ctx.player.sendMessage(TextUtil.parse(
                "<green>Loaded kit <yellow>" + kit.getName() + "</yellow> into your inventory. " +
                        "<gray>(" + (kit.isPlayerMade() ? "player kit" : "admin kit") + ")"));
    }

    // /kit delete <name>
    private void runDelete(CommandContext ctx) {
        if (ctx.args.length < 2) {
            ctx.player.sendMessage(TextUtil.parse("<red>Usage: /kit delete <name>"));
            return;
        }
        String name = ctx.args[1];
        // Try admin kit first, then player kit
        if (!kitManager.deleteKit(name, false) && !kitManager.deleteKit(name, true)) {
            ctx.player.sendMessage(TextUtil.parse(
                    "<red>No kit named <yellow>" + name + "</yellow> found."));
            return;
        }
        ctx.player.sendMessage(TextUtil.parse(
                "<green>Kit <yellow>" + name + "</yellow> deleted."));
    }

    // /kit list
    private void runList(CommandContext ctx) {
        var kits = kitManager.getAllKits();
        if (kits.isEmpty()) {
            ctx.player.sendMessage(TextUtil.parse("<yellow>No kits configured yet."));
            return;
        }
        ctx.player.sendMessage(TextUtil.parse("<green>--- Kits ---"));
        for (Kit k : kits) {
            String type = k.isPlayerMade() ? "<gray>[player]" : "<aqua>[admin]";
            ctx.player.sendMessage(TextUtil.parse(
                    type + " </><white>" + k.getName() +
                            "</white> <gray>| " + countItems(k) + " stacks"));
        }
    }

    // /kit config <name>
    // Stub — you'll fill this in with custom configuration later
    private void runConfig(CommandContext ctx) {
        if (ctx.args.length < 2) {
            ctx.player.sendMessage(TextUtil.parse("<red>Usage: /kit config <name>"));
            return;
        }
        Kit kit = kitManager.getKit(ctx.args[1]);
        if (kit == null) {
            ctx.player.sendMessage(TextUtil.parse(
                    "<red>No kit named <yellow>" + ctx.args[1] + "</yellow> found."));
            return;
        }
        // TODO: open config UI / display config options
        ctx.player.sendMessage(TextUtil.parse(
                "<yellow>Config for kit <white>" + kit.getName() +
                        "</white> — not yet implemented."));
    }

    // /kit icon <kit>
    private void runIcon(CommandContext ctx) {
        if (ctx.args.length < 2) {
            ctx.player.sendMessage(TextUtil.parse("<red>Usage: /kit icon <name>"));
            return;
        }
        ItemStack held = ctx.player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            ctx.player.sendMessage(TextUtil.parse("<red>Hold an item to set as the kit icon."));
            return;
        }
        if (!kitManager.setIcon(ctx.player, ctx.args[1])) {
            ctx.player.sendMessage(TextUtil.parse(
                    "<red>No kit named <yellow>" + ctx.args[1] + "</yellow> found."));
            return;
        }
        ctx.player.sendMessage(TextUtil.parse(
                "<green>Icon for kit <yellow>" + ctx.args[1] + "</yellow> set to <white>" +
                        held.getType().name().toLowerCase() + "</white>."));
    }

    private void runRounds(CommandContext ctx) {
        if (ctx.args.length < 3) {
            ctx.player.sendMessage(TextUtil.parse("<red>Usage: /kit rounds <name> <number>"));
            return;
        }
        int rounds;
        try { rounds = Integer.parseInt(ctx.args[2]); }
        catch (NumberFormatException e) {
            ctx.player.sendMessage(TextUtil.parse("<red>Invalid number."));
            return;
        }
        if (rounds < 1 || rounds > 99) {
            ctx.player.sendMessage(TextUtil.parse("<red>Rounds must be between 1 and 99."));
            return;
        }
        if (!kitManager.setRounds(ctx.args[1], rounds)) {
            ctx.player.sendMessage(TextUtil.parse("<red>Kit not found."));
            return;
        }
        ctx.player.sendMessage(TextUtil.parse(
                "<green>Kit <yellow>" + ctx.args[1] + "</yellow> default rounds set to <white>" + rounds + "</white>."));
    }

    private int countItems(Kit kit) {
        int count = 0;
        if (kit.getContents() != null)
            for (var i : kit.getContents()) if (i != null && !i.getType().isAir()) count++;
        if (kit.getArmor() != null)
            for (var i : kit.getArmor()) if (i != null && !i.getType().isAir()) count++;
        if (kit.getOffhand() != null && !kit.getOffhand().getType().isAir()) count++;
        return count;
    }
}