package net.tutla.pvpPlus.commandSystem.command;

import net.tutla.pvpPlus.commandSystem.*;
import net.tutla.pvpPlus.fight.FightManager;
import net.tutla.pvpPlus.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.List;

public class SpectateCommand extends TutlaCommand {

    private final FightManager fightManager;

    public SpectateCommand(FightManager fightManager) {
        super("spectate", "/spectate <player>", "Spectate a fight",
                CommandSection.CONTROLS,
                new CommandTabAutoComplete("spectate", List.of(), "<player>"));
        this.fightManager = fightManager;
    }

    @Override
    public boolean run(CommandContext ctx) {
        if (ctx.args.length == 0) return false;
        Player target = Bukkit.getPlayer(ctx.args[0]);
        if (target == null) {
            ctx.player.sendMessage(TextUtil.parse("<red>Player not found."));
            return true;
        }
        if (!fightManager.addSpectator(ctx.player, target)) {
            ctx.player.sendMessage(TextUtil.parse(
                    "<red>That player is not in a fight, or you are already in one."));
        }
        return true;
    }
}
