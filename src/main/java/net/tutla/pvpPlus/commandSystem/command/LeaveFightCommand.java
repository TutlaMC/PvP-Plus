package net.tutla.pvpPlus.commandSystem.command;

import net.tutla.pvpPlus.commandSystem.*;
import net.tutla.pvpPlus.fight.FightManager;
import net.tutla.pvpPlus.util.TextUtil;
import java.util.List;

public class LeaveFightCommand extends TutlaCommand {

    private final FightManager fightManager;

    public LeaveFightCommand(FightManager fightManager) {
        super("leavefight", "/leavefight", "Leave a spectated fight",
                CommandSection.CONTROLS,
                new CommandTabAutoComplete("leavefight", List.of(), ""));
        this.fightManager = fightManager;
    }

    @Override
    public boolean run(CommandContext ctx) {
        if (fightManager.isSpectating(ctx.player)) {
            fightManager.removeSpectator(ctx.player);
            ctx.player.sendMessage(TextUtil.parse("<yellow>Left spectator mode."));
            return true;
        }
        ctx.player.sendMessage(TextUtil.parse("<red>You are not spectating a fight."));
        return true;
    }
}
