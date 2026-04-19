package net.tutla.pvpPlus.party;

import net.tutla.pvpPlus.arena.Arena;
import net.tutla.pvpPlus.kit.Kit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;

public class PartyManager {

    // player UUID -> their party
    private final Map<UUID, Party> playerParty = new HashMap<>();

    // pending invites: invited UUID -> inviter's party
    private final Map<UUID, Party> pendingInvites = new HashMap<>();
    public record PartyDuelRequest(Party challenger, Kit kit, Arena arena, int rounds) {}

    // pending party duels: challenged party id -> challenger party
    private final Map<UUID, PartyDuelRequest> pendingDuels = new HashMap<>();

    // -------------------------------------------------------------------------
    // Party lifecycle
    // -------------------------------------------------------------------------

    /** Returns null if player already in a party. */
    public Party createParty(Player leader) {
        if (isInParty(leader)) return null;
        Party party = new Party(leader);
        playerParty.put(leader.getUniqueId(), party);
        return party;
    }

    public boolean disbandParty(Player leader) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader.getUniqueId())) return false;
        // Notify and remove all members
        for (UUID uuid : party.getMembers()) {
            playerParty.remove(uuid);
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && !member.equals(leader)) {
                member.sendMessage(net.tutla.pvpPlus.util.TextUtil.parse(
                        "<red>The party has been disbanded by the leader."));
            }
        }
        // Clean up any pending invites pointing to this party
        pendingInvites.entrySet().removeIf(e -> e.getValue().getId().equals(party.getId()));
        pendingDuels.entrySet().removeIf(e ->
                e.getValue().challenger().getId().equals(party.getId()) ||
                        e.getKey().equals(party.getId()));
        return true;
    }

    public boolean leaveParty(Player player) {
        Party party = getParty(player);
        if (party == null || party.isLeader(player.getUniqueId())) return false; // leader must disband
        party.removeMember(player.getUniqueId());
        playerParty.remove(player.getUniqueId());
        broadcastToParty(party, "<yellow>" + player.getName() + " has left the party.");
        // If only one person left, auto-disband
        if (party.size() == 1) {
            Player remaining = Bukkit.getPlayer(party.getMembers().iterator().next());
            if (remaining != null) {
                remaining.sendMessage(net.tutla.pvpPlus.util.TextUtil.parse(
                        "<yellow>You are the only member left. The party has been disbanded."));
            }
            playerParty.remove(party.getLeader());
        }
        return true;
    }

    public boolean kickMember(Player leader, Player target) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader.getUniqueId())) return false;
        if (!party.hasMember(target.getUniqueId())) return false;
        if (target.equals(leader)) return false;
        party.removeMember(target.getUniqueId());
        playerParty.remove(target.getUniqueId());
        target.sendMessage(net.tutla.pvpPlus.util.TextUtil.parse(
                "<red>You have been kicked from the party."));
        broadcastToParty(party, "<yellow>" + target.getName() + " was kicked from the party.");
        return true;
    }

    // -------------------------------------------------------------------------
    // Invites
    // -------------------------------------------------------------------------

    public enum InviteResult { SENT, ALREADY_IN_PARTY, TARGET_IN_PARTY, ALREADY_INVITED, NOT_LEADER }

    public InviteResult invite(Player leader, Player target) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader.getUniqueId())) return InviteResult.NOT_LEADER;
        if (isInParty(target)) return InviteResult.TARGET_IN_PARTY;
        if (pendingInvites.containsKey(target.getUniqueId())) return InviteResult.ALREADY_INVITED;
        pendingInvites.put(target.getUniqueId(), party);
        return InviteResult.SENT;
    }

    public boolean acceptInvite(Player player) {
        Party party = pendingInvites.remove(player.getUniqueId());
        if (party == null) return false;
        party.addMember(player.getUniqueId());
        playerParty.put(player.getUniqueId(), party);
        broadcastToParty(party, "<green>" + player.getName() + " has joined the party!");
        return true;
    }

    public boolean denyInvite(Player player) {
        Party party = pendingInvites.remove(player.getUniqueId());
        if (party == null) return false;
        Player leader = Bukkit.getPlayer(party.getLeader());
        if (leader != null) {
            leader.sendMessage(net.tutla.pvpPlus.util.TextUtil.parse(
                    "<yellow>" + player.getName() + " denied your party invite."));
        }
        return true;
    }

    public boolean hasPendingInvite(Player player) {
        return pendingInvites.containsKey(player.getUniqueId());
    }

    // -------------------------------------------------------------------------
    // Party chat
    // -------------------------------------------------------------------------

    public boolean togglePartyChat(Player player) {
        Party party = getParty(player);
        if (party == null) return false;
        party.togglePartyChat(player.getUniqueId());
        return true;
    }

    public boolean hasPartyChatActive(Player player) {
        Party party = getParty(player);
        return party != null && party.hasPartyChat(player.getUniqueId());
    }

    public void broadcastToParty(Party party, String miniMessage) {
        for (UUID uuid : party.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                member.sendMessage(net.tutla.pvpPlus.util.TextUtil.parse(
                        "<dark_aqua>[Party] </dark_aqua>" + miniMessage));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Party duels
    // -------------------------------------------------------------------------

    public enum DuelResult { SENT, NOT_IN_PARTY, NOT_LEADER, TARGET_NOT_FOUND, ALREADY_PENDING }

    public DuelResult sendDuel(Player leader, String targetPartyLeaderName, Kit kit, Arena arena, int rounds) {
        Party challenger = getParty(leader);
        if (challenger == null) return DuelResult.NOT_IN_PARTY;
        if (!challenger.isLeader(leader.getUniqueId())) return DuelResult.NOT_LEADER;

        Player targetLeader = Bukkit.getPlayer(targetPartyLeaderName);
        if (targetLeader == null) return DuelResult.TARGET_NOT_FOUND;
        Party challenged = getParty(targetLeader);
        if (challenged == null) return DuelResult.TARGET_NOT_FOUND;

        if (pendingDuels.containsKey(challenged.getId())) return DuelResult.ALREADY_PENDING;
        pendingDuels.put(challenged.getId(), new PartyDuelRequest(challenger, kit, arena, rounds));
        return DuelResult.SENT;
    }

    public PartyDuelRequest acceptDuel(Player leader) {
        Party challenged = getParty(leader);
        if (challenged == null || !challenged.isLeader(leader.getUniqueId())) return null;
        return pendingDuels.remove(challenged.getId());
    }

    public boolean denyDuel(Player leader) {
        Party challenged = getParty(leader);
        if (challenged == null || !challenged.isLeader(leader.getUniqueId())) return false;
        PartyDuelRequest request = pendingDuels.remove(challenged.getId());
        if (request == null) return false;
        Player challengerLeader = Bukkit.getPlayer(request.challenger().getLeader());
        if (challengerLeader != null) {
            challengerLeader.sendMessage(net.tutla.pvpPlus.util.TextUtil.parse(
                    "<red>Your party duel was denied."));
        }
        return true;
    }

    public boolean hasPendingDuel(Player leader) {
        Party party = getParty(leader);
        return party != null && pendingDuels.containsKey(party.getId());
    }

    // -------------------------------------------------------------------------
    // Lookup helpers
    // -------------------------------------------------------------------------

    public List<Player> getAllPartyLeaders(){
        return playerParty.keySet().stream().map(Bukkit::getPlayer).toList();
    }

    public List<String> getAllPartyLeaderNames(){
        return playerParty.keySet().stream().map((uuid -> {return Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName();})).toList();
    }

    public Party getParty(Player player) {
        return playerParty.get(player.getUniqueId());
    }

    public boolean isInParty(Player player) {
        return playerParty.containsKey(player.getUniqueId());
    }

    /** Gets all members as online Players, skipping offline ones. */
    public List<Player> getOnlineMembers(Party party) {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) result.add(p);
        }
        return result;
    }
}