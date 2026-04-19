package net.tutla.pvpPlus.commandSystem.command;

import net.tutla.pvpPlus.commandSystem.*;
import net.tutla.pvpPlus.gui.QueueGui;
import net.tutla.pvpPlus.kit.KitManager;
import net.tutla.pvpPlus.queue.QueueManager;
import net.tutla.pvpPlus.util.TextUtil;
import java.util.List;

public class QueueCommand extends TutlaCommand {

    private final QueueManager queueManager;
    private final KitManager kitManager;

    public QueueCommand(QueueManager queueManager, KitManager kitManager) {
        super("queue", "/queue <join|leave|status|gui>", "Join the matchmaking queue",
                CommandSection.CONTROLS,
                new CommandTabAutoComplete("queue",
                        List.of(
                                new CommandTabAutoComplete("join",   List.of(), ""),
                                new CommandTabAutoComplete("leave",  List.of(), ""),
                                new CommandTabAutoComplete("status", List.of(), ""),
                                new CommandTabAutoComplete("gui",    List.of(), "")
                        ),
                        "<values>"
                ).setValues(List.of("join", "leave", "status", "gui"))
        );
        this.queueManager = queueManager;
        this.kitManager = kitManager;
    }

    @Override
    public boolean run(CommandContext ctx) {
        if (ctx.args.length == 0) return false;
        switch (ctx.args[0].toLowerCase()) {
            case "join"   -> runJoin(ctx);
            case "leave"  -> runLeave(ctx);
            case "status" -> runStatus(ctx);
            case "gui"    -> runGui(ctx);
            default       -> { return false; }
        }
        return true;
    }

    private void runJoin(CommandContext ctx) {
        // /queue join with no kit arg opens GUI, or defaults to first kit
        var kit = kitManager.getServerKits().stream().findFirst().orElse(null);
        if (kit == null) {
            ctx.player.sendMessage(TextUtil.parse("<red>No kits available. An admin needs to create one."));
            return;
        }
        switch (queueManager.joinQueue(ctx.player, kit)) {
            case JOINED ->
                    ctx.player.sendMessage(TextUtil.parse(
                            "<green>Joined queue for <yellow>" + kit.getName() +
                                    "</yellow>. Waiting for opponent..."));
            case ALREADY_IN_QUEUE ->
                    ctx.player.sendMessage(TextUtil.parse("<red>You're already in a queue."));
            case IN_FIGHT ->
                    ctx.player.sendMessage(TextUtil.parse("<red>You're already in a fight."));
            case NO_KIT ->
                    ctx.player.sendMessage(TextUtil.parse("<red>Invalid kit."));
        }
    }

    private void runLeave(CommandContext ctx) {
        if (!queueManager.leaveQueue(ctx.player)) {
            ctx.player.sendMessage(TextUtil.parse("<red>You're not in a queue."));
            return;
        }
        ctx.player.sendMessage(TextUtil.parse("<yellow>Left the queue."));
    }

    private void runStatus(CommandContext ctx) {
        if (!queueManager.isInQueue(ctx.player)) {
            ctx.player.sendMessage(TextUtil.parse("<yellow>You are not in any queue."));
            return;
        }
        String kitName = queueManager.getQueuedKit(ctx.player);
        int size = queueManager.getQueueSize(kitName);
        ctx.player.sendMessage(TextUtil.parse(
                "<green>Queue: <yellow>" + kitName +
                        " <gray>| Players waiting: <white>" + size));
    }

    private void runGui(CommandContext ctx) {
        QueueGui.open(ctx.player, queueManager, kitManager);
    }
}