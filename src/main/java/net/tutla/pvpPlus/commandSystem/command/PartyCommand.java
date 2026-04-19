package net.tutla.pvpPlus.commandSystem.command;

import net.tutla.pvpPlus.PvpPlus;
import net.tutla.pvpPlus.arena.ArenaManager;
import net.tutla.pvpPlus.commandSystem.*;
import net.tutla.pvpPlus.fight.FightManager;
import net.tutla.pvpPlus.fight.FightTeam;
import net.tutla.pvpPlus.fight.FightType;
import net.tutla.pvpPlus.gui.DuelGui;
import net.tutla.pvpPlus.kit.KitManager;
import net.tutla.pvpPlus.party.Party;
import net.tutla.pvpPlus.party.PartyManager;
import net.tutla.pvpPlus.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PartyCommand extends TutlaCommand {

    private final PartyManager partyManager;
    private final FightManager fightManager;
    private final KitManager kitManager;
    private final ArenaManager arenaManager;

    public PartyCommand(PartyManager partyManager, FightManager fightManager,
                        KitManager kitManager, ArenaManager arenaManager) {
        super("party", "/party <subcommand>", "Manage your party",
                CommandSection.CONTROLS,
                new CommandTabAutoComplete("party",
                        List.of(
                                new CommandTabAutoComplete("create",  List.of(), ""),
                                new CommandTabAutoComplete("invite",  List.of(), "<player>"),
                                new CommandTabAutoComplete("accept",  List.of(), ""),
                                new CommandTabAutoComplete("deny",    List.of(), ""),
                                new CommandTabAutoComplete("leave",   List.of(), ""),
                                new CommandTabAutoComplete("kick",    List.of(), "<player>"),
                                new CommandTabAutoComplete("disband", List.of(), ""),
                                new CommandTabAutoComplete("list",    List.of(), ""),
                                new CommandTabAutoComplete("chat",    List.of(), ""),
                                new CommandTabAutoComplete("duel",    List.of(), "<leader>"),
                                new CommandTabAutoComplete("split",   List.of(), ""),
                                new CommandTabAutoComplete("ffa",     List.of(), "")
                        ),
                        "<values>"
                ).setValues(List.of(
                        "create","invite","accept","deny","leave",
                        "kick","disband","list","chat","duel","split","ffa"
                ))
        );
        this.partyManager = partyManager;
        this.fightManager = fightManager;
        this.kitManager = kitManager;
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean run(CommandContext ctx) {
        if (ctx.args.length == 0) return false;
        switch (ctx.args[0].toLowerCase()) {
            case "create"  -> runCreate(ctx);
            case "invite"  -> runInvite(ctx);
            case "accept"  -> runAccept(ctx);
            case "deny"    -> runDeny(ctx);
            case "leave"   -> runLeave(ctx);
            case "kick"    -> runKick(ctx);
            case "disband" -> runDisband(ctx);
            case "list"    -> runList(ctx);
            case "chat"    -> runChat(ctx);
            case "duel"    -> runDuel(ctx);
            case "split"   -> runSplit(ctx);
            case "ffa"     -> runFfa(ctx);
            default        -> { return false; }
        }
        return true;
    }

    private void runCreate(CommandContext ctx) {
        if (partyManager.isInParty(ctx.player)) {
            ctx.player.sendMessage(TextUtil.parse("<red>You're already in a party."));
            return;
        }
        Party party = partyManager.createParty(ctx.player);
        if (party == null) {
            ctx.player.sendMessage(TextUtil.parse("<red>Could not create party."));
            return;
        }
        ctx.player.sendMessage(TextUtil.parse(
                "<green>Party created! Invite players with <yellow>/party invite <player></yellow>."));
    }

    private void runInvite(CommandContext ctx) {
        if (ctx.args.length < 2) {
            ctx.player.sendMessage(TextUtil.parse("<red>Usage: /party invite <player>"));
            return;
        }
        Player target = Bukkit.getPlayer(ctx.args[1]);
        if (target == null) {
            ctx.player.sendMessage(TextUtil.parse("<red>Player not found."));
            return;
        }
        switch (partyManager.invite(ctx.player, target)) {
            case SENT -> {
                ctx.player.sendMessage(TextUtil.parse(
                        "<green>Invite sent to <yellow>" + target.getName() + "</yellow>."));
                target.sendMessage(TextUtil.parse(
                        "<green>" + ctx.player.getName() + " has invited you to their party!\n" +
                                "<gray>Use <white>/party accept</white> or <white>/party deny</white>."));
            }
            case NOT_LEADER ->
                    ctx.player.sendMessage(TextUtil.parse("<red>Only the party leader can invite players."));
            case TARGET_IN_PARTY ->
                    ctx.player.sendMessage(TextUtil.parse("<red>That player is already in a party."));
            case ALREADY_INVITED ->
                    ctx.player.sendMessage(TextUtil.parse("<red>That player already has a pending invite."));
            case ALREADY_IN_PARTY ->
                    ctx.player.sendMessage(TextUtil.parse("<red>You are not in a party."));
        }
    }

    private void runAccept(CommandContext ctx) {
        if (!partyManager.hasPendingInvite(ctx.player)) {
            ctx.player.sendMessage(TextUtil.parse("<red>You have no pending party invite."));
            return;
        }
        partyManager.acceptInvite(ctx.player);
    }

    private void runDeny(CommandContext ctx) {
        // Check if this is a duel deny or invite deny
        if (ctx.args.length >= 2 && ctx.args[1].equalsIgnoreCase("duel")) {
            // handled under /party duel deny — redirect
            runDuelDeny(ctx);
            return;
        }
        if (!partyManager.hasPendingInvite(ctx.player)) {
            ctx.player.sendMessage(TextUtil.parse("<red>You have no pending party invite."));
            return;
        }
        partyManager.denyInvite(ctx.player);
        ctx.player.sendMessage(TextUtil.parse("<yellow>Party invite denied."));
    }

    private void runLeave(CommandContext ctx) {
        Party party = partyManager.getParty(ctx.player);
        if (party == null) {
            ctx.player.sendMessage(TextUtil.parse("<red>You are not in a party."));
            return;
        }
        if (party.isLeader(ctx.player.getUniqueId())) {
            ctx.player.sendMessage(TextUtil.parse(
                    "<red>You are the leader. Use <yellow>/party disband</yellow> instead."));
            return;
        }
        partyManager.leaveParty(ctx.player);
        ctx.player.sendMessage(TextUtil.parse("<yellow>You left the party."));
    }

    private void runKick(CommandContext ctx) {
        if (ctx.args.length < 2) {
            ctx.player.sendMessage(TextUtil.parse("<red>Usage: /party kick <player>"));
            return;
        }
        Player target = Bukkit.getPlayer(ctx.args[1]);
        if (target == null) {
            ctx.player.sendMessage(TextUtil.parse("<red>Player not found."));
            return;
        }
        if (!partyManager.kickMember(ctx.player, target)) {
            ctx.player.sendMessage(TextUtil.parse(
                    "<red>Could not kick that player. Are you the leader and is that player in your party?"));
            return;
        }
        ctx.player.sendMessage(TextUtil.parse(
                "<yellow>Kicked <white>" + target.getName() + "</white> from the party."));
    }

    private void runDisband(CommandContext ctx) {
        if (!partyManager.disbandParty(ctx.player)) {
            ctx.player.sendMessage(TextUtil.parse(
                    "<red>You are not a party leader."));
            return;
        }
        ctx.player.sendMessage(TextUtil.parse("<yellow>Party disbanded."));
    }

    private void runList(CommandContext ctx) {
        Party party = partyManager.getParty(ctx.player);
        if (party == null) {
            ctx.player.sendMessage(TextUtil.parse("<red>You are not in a party."));
            return;
        }
        ctx.player.sendMessage(TextUtil.parse("<dark_aqua>--- Party Members ---"));
        for (java.util.UUID uuid : party.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            String name  = member != null ? member.getName() : uuid.toString();
            String role  = party.isLeader(uuid) ? " <gold>[Leader]" : "";
            String online = member != null ? "<green>●" : "<gray>●";
            ctx.player.sendMessage(TextUtil.parse(online + " <white>" + name + role));
        }
    }

    private void runChat(CommandContext ctx) {
        if (!partyManager.isInParty(ctx.player)) {
            ctx.player.sendMessage(TextUtil.parse("<red>You are not in a party."));
            return;
        }
        partyManager.togglePartyChat(ctx.player);
        boolean active = partyManager.hasPartyChatActive(ctx.player);
        ctx.player.sendMessage(TextUtil.parse(
                "<dark_aqua>Party chat " + (active ? "<green>enabled" : "<gray>disabled") + "<dark_aqua>."));
    }

    private void runDuel(CommandContext ctx) {
        if (ctx.args.length < 2) {
            ctx.player.sendMessage(TextUtil.parse("<red>Usage: /party duel <leader name>"));
            return;
        }
        // /party duel accept
        if (ctx.args[1].equalsIgnoreCase("accept")) {
            runDuelAccept(ctx);
            return;
        }
        // /party duel deny
        if (ctx.args[1].equalsIgnoreCase("deny")) {
            runDuelDeny(ctx);
            return;
        }
        // /party duel <player>
        Player opposer = Bukkit.getPlayer(ctx.args[1]);
        if (opposer != null){
            if (ctx.player.getUniqueId() == opposer.getUniqueId()){
                ctx.player.sendMessage(TextUtil.parse("<red>You cannot send it to your party!"));
                return;
            }
        }

        switch (partyManager.sendDuel(ctx.player, ctx.args[1])) {
            case SENT -> {
                Party challenged = partyManager.getParty(opposer);
                partyManager.broadcastToParty(challenged,
                        "<yellow>" + ctx.player.getName() + "'s party has challenged you to a duel!\n" +
                                "<gray>Leader: use <white>/party duel accept</white> or <white>/party duel deny</white>.");
                ctx.player.sendMessage(TextUtil.parse("<green>Party duel challenge sent!"));
            }
            case NOT_IN_PARTY ->
                    ctx.player.sendMessage(TextUtil.parse("<red>You are not in a party."));
            case NOT_LEADER ->
                    ctx.player.sendMessage(TextUtil.parse("<red>Only the party leader can send duel challenges."));
            case TARGET_NOT_FOUND ->
                    ctx.player.sendMessage(TextUtil.parse("<red>Party leader not found."));
            case ALREADY_PENDING ->
                    ctx.player.sendMessage(TextUtil.parse("<red>That party already has a pending duel request."));
        }
    }

    private void runDuelAccept(CommandContext ctx) {
        Party challenger = partyManager.acceptDuel(ctx.player);
        if (challenger == null) {
            ctx.player.sendMessage(TextUtil.parse("<red>No pending party duel to accept."));
            return;
        }
        Party challenged = partyManager.getParty(ctx.player);

        partyManager.broadcastToParty(challenger, "<green>Your party duel was accepted! Get ready.");
        partyManager.broadcastToParty(challenged, "<green>Party duel accepted! Get ready.");

        // Open duel config GUI for the accepting leader — they pick kit/arena/rounds
        // The challenger's leader is the "target" reference we pass as the opposing side
        Player challengerLeader = Bukkit.getPlayer(challenger.getLeader());
        DuelGui.open(ctx.player,
                PvpPlus.getInstance().getDuelManager(),
                kitManager,
                challengerLeader,   // target display only — actual teams are built from parties
                true                // enemyIsTeam = true, so FightManager builds team lists from parties
        );
        // When the GUI sends, DuelManager.sendPartyDuel() is called instead of sendDuel()
        // We handle that in DuelGui by checking enemyIsTeam — see DuelGui changes below
    }

    private void runDuelDeny(CommandContext ctx) {
        if (!partyManager.denyDuel(ctx.player)) {
            ctx.player.sendMessage(TextUtil.parse("<red>No pending party duel to deny."));
            return;
        }
        ctx.player.sendMessage(TextUtil.parse("<yellow>Party duel denied."));
    }

    private void runSplit(CommandContext ctx) {
        Party party = partyManager.getParty(ctx.player);
        if (party == null) {
            ctx.player.sendMessage(TextUtil.parse("<red>You are not in a party."));
            return;
        }
        if (!party.isLeader(ctx.player.getUniqueId())) {
            ctx.player.sendMessage(TextUtil.parse("<red>Only the leader can start a split fight."));
            return;
        }
        if (party.size() < 2) {
            ctx.player.sendMessage(TextUtil.parse("<red>Need at least 2 members to split."));
            return;
        }

        List<UUID> members = new ArrayList<>(party.getMembers());
        Collections.shuffle(members);
        int half = members.size() / 2;

        List<FightTeam> teams = List.of(
                new FightTeam("Team Red",  new ArrayList<>(members.subList(0, half))),
                new FightTeam("Team Blue", new ArrayList<>(members.subList(half, members.size())))
        );

        DuelGui.openForTeams(ctx.player,
                PvpPlus.getInstance().getDuelManager(),
                kitManager, teams, FightType.SPLIT);
    }

    private void runFfa(CommandContext ctx) {
        Party party = partyManager.getParty(ctx.player);
        if (party == null) {
            ctx.player.sendMessage(TextUtil.parse("<red>You are not in a party."));
            return;
        }
        if (!party.isLeader(ctx.player.getUniqueId())) {
            ctx.player.sendMessage(TextUtil.parse("<red>Only the leader can start an FFA."));
            return;
        }
        if (party.size() < 2) {
            ctx.player.sendMessage(TextUtil.parse("<red>Need at least 2 members for an FFA."));
            return;
        }

        List<FightTeam> teams = party.getMembers().stream()
                .map(uuid -> {
                    Player p = Bukkit.getPlayer(uuid);
                    String name = p != null ? p.getName() : uuid.toString().substring(0, 8);
                    return new FightTeam(name, List.of(uuid));
                })
                .collect(Collectors.toList());

        DuelGui.openForTeams(ctx.player,
                PvpPlus.getInstance().getDuelManager(),
                kitManager, teams, FightType.FFA);
    }
}
