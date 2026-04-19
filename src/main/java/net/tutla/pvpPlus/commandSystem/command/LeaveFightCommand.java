package net.tutla.pvpPlus.commandSystem.command;

import net.tutla.pvpPlus.commandSystem.*;
import net.tutla.pvpPlus.fight.Fight;
import net.tutla.pvpPlus.fight.FightManager;
import net.tutla.pvpPlus.fight.FightTeam;
import net.tutla.pvpPlus.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        }

        if (fightManager.isInFight(ctx.player)) {
            Fight fight = fightManager.getFight(ctx.player);

            // Snapshot participants BEFORE leaveFight modifies state
            List<UUID> participants = new ArrayList<>(fight.getAllParticipants());

            ctx.player.sendMessage(TextUtil.parse("<yellow>You forfeited."));

            // Notify others
            for (UUID uuid : participants) {
                if (uuid.equals(ctx.player.getUniqueId())) continue;
                Player other = Bukkit.getPlayer(uuid);
                if (other != null) {
                    other.sendMessage(TextUtil.parse(
                            "<aqua>" + ctx.player.getName() + " <yellow>forfeited."));
                }
            }

            fightManager.leaveFight(ctx.player);
            return true;
        }

        ctx.player.sendMessage(TextUtil.parse("<red>You are not in a fight or spectating."));
        return true;
    }
}
