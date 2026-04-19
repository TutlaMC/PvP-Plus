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
                        List.of(),
                        ""
                )
        );
        this.queueManager = queueManager;
        this.kitManager = kitManager;
    }

    @Override
    public boolean run(CommandContext ctx) {
        QueueGui.open(ctx.player, queueManager, kitManager);
        return true;
    }

}