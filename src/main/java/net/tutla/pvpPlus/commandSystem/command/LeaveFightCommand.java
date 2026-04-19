package net.tutla.pvpPlus.commandSystem.command;

import net.tutla.pvpPlus.commandSystem.*;
import net.tutla.pvpPlus.fight.Fight;
import net.tutla.pvpPlus.fight.FightManager;
import net.tutla.pvpPlus.fight.FightTeam;
import net.tutla.pvpPlus.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class LeaveFightCommand extends TutlaCommand {

    private final FightManager fightManager;

    public LeaveFightCommand(FightManager fightManager) {
        super("leavefight", "/leavefight", "Leave a fight",
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
        } else if (fightManager.isInFight(ctx.player)){
            ctx.player.sendMessage(TextUtil.parse("<yellow>You forfeited"));
            fightManager.getFight(ctx.player).getAllParticipants().forEach((uuid)-> {
                Player player = Bukkit.getPlayer(uuid);
                player.sendMessage(TextUtil.parse("<cyan>"+ctx.player.getName()+" <yellow> forfeited"));
            });
            fightManager.leaveFight(ctx.player);
            return true;
        }
        ctx.player.sendMessage(TextUtil.parse("<red>You are not spectating a fight."));
        return true;
    }
}
