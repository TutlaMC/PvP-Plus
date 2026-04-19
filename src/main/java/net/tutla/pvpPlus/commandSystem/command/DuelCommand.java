package net.tutla.pvpPlus.commandSystem.command;

import net.tutla.pvpPlus.commandSystem.*;
import net.tutla.pvpPlus.duel.DuelManager;
import net.tutla.pvpPlus.fight.FightManager;
import net.tutla.pvpPlus.gui.DuelGui;
import net.tutla.pvpPlus.kit.KitManager;
import net.tutla.pvpPlus.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class DuelCommand extends TutlaCommand {

    private final DuelManager duelManager;
    private final FightManager fightManager;
    private final KitManager kitManager;

    public DuelCommand(DuelManager duelManager, FightManager fightManager, KitManager kitManager) {
        super("duel", "/duel <player|accept|deny|gui|toggle>", "Challenge players to duels",
                CommandSection.CONTROLS,
                new CommandTabAutoComplete("duel",
                        List.of(
                                new CommandTabAutoComplete("accept", List.of(), ""),
                                new CommandTabAutoComplete("deny",   List.of(), ""),
                                new CommandTabAutoComplete("gui",    List.of(), ""),
                                new CommandTabAutoComplete("toggle", List.of(), "")
                        ),
                        "<subcommand|player>"
                ).setValues(List.of("accept", "deny", "gui", "toggle"))
        );
        this.duelManager = duelManager;
        this.fightManager = fightManager;
        this.kitManager = kitManager;
    }

    @Override
    public boolean run(CommandContext ctx) {
        if (ctx.args.length == 0) return false;
        switch (ctx.args[0].toLowerCase()) {
            case "accept" -> runAccept(ctx);
            case "deny"   -> runDeny(ctx);
            case "gui"    -> runGui(ctx);
            case "toggle" -> runToggle(ctx);
            default       -> runChallenge(ctx);
        }
        return true;
    }

    private void runChallenge(CommandContext ctx) {
        Player target = Bukkit.getPlayer(ctx.args[0]);
        if (target == null) {
            ctx.player.sendMessage(TextUtil.parse("<red>Player not found."));
            return;
        }
        // Default: use first available kit, auto arena, kit's default rounds
        var kit = kitManager.getServerKits().stream().findFirst().orElse(null);
        if (kit == null) {
            ctx.player.sendMessage(TextUtil.parse("<red>No kits configured. Use /duel gui to configure."));
            return;
        }

        switch (duelManager.sendDuel(ctx.player, target, kit, null, kit.getDefaultRounds())) {
            case SENT -> {
                ctx.player.sendMessage(TextUtil.parse(
                        "<green>Duel request sent to <yellow>" + target.getName() + "</yellow>. " +
                                "<gray>(30s to accept)"));
                target.sendMessage(TextUtil.parse(
                        "<yellow>" + ctx.player.getName() + " <green>challenged you to a duel!\n" +
                                "<gray>Kit: <white>" + kit.getName() +
                                " <gray>| Rounds: <white>" + kit.getDefaultRounds() + "\n" +
                                "<gray>Use <white>/duel accept</white> or <white>/duel deny</white>."));
            }
            case SELF_DUEL ->
                    ctx.player.sendMessage(TextUtil.parse("<red>You can't duel yourself."));
            case TARGET_BUSY ->
                    ctx.player.sendMessage(TextUtil.parse("<red>That player is already in a fight."));
            case SENDER_BUSY ->
                    ctx.player.sendMessage(TextUtil.parse("<red>You are already in a fight."));
            case REQUESTS_DISABLED ->
                    ctx.player.sendMessage(TextUtil.parse("<red>That player has duel requests disabled."));
            case ALREADY_PENDING ->
                    ctx.player.sendMessage(TextUtil.parse("<red>That player already has a pending duel request."));
            case NO_ARENA ->
                    ctx.player.sendMessage(TextUtil.parse("<red>No arenas are available right now."));
        }
    }

    private void runAccept(CommandContext ctx) {
        if (!duelManager.hasPendingRequest(ctx.player)) {
            ctx.player.sendMessage(TextUtil.parse("<red>No pending duel request."));
            return;
        }
        if (!duelManager.acceptDuel(ctx.player)) {
            ctx.player.sendMessage(TextUtil.parse("<red>Could not start duel — arena may be unavailable."));
            return;
        }
    }

    private void runDeny(CommandContext ctx) {
        if (!duelManager.denyDuel(ctx.player)) {
            ctx.player.sendMessage(TextUtil.parse("<red>No pending duel request to deny."));
            return;
        }
        ctx.player.sendMessage(TextUtil.parse("<yellow>Duel request denied."));
    }

    private void runGui(CommandContext ctx) {
        // Opens the duel configuration GUI — handled next
        DuelGui.open(ctx.player, duelManager, kitManager, null);
    }

    private void runToggle(CommandContext ctx) {
        boolean nowEnabled = duelManager.toggleDuelRequests(ctx.player);
        ctx.player.sendMessage(TextUtil.parse(
                "<yellow>Duel requests " + (nowEnabled ? "<green>enabled" : "<red>disabled") + "<yellow>."));
    }
}
