package net.tutla.pvpPlus.party;

import org.bukkit.entity.Player;
import java.util.*;

public class Party {

    private final UUID id = UUID.randomUUID();
    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>(); // preserves join order
    private boolean chatEnabled = false; // per-party global toggle (leader sets it)

    // Players who have party chat toggled on individually
    private final Set<UUID> partyChatActive = new HashSet<>();

    public Party(Player leader) {
        this.leader = leader.getUniqueId();
        members.add(leader.getUniqueId());
    }

    public UUID getId() { return id; }
    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }

    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }
    public void addMember(UUID uuid) { members.add(uuid); }
    public void removeMember(UUID uuid) { members.remove(uuid); }
    public boolean hasMember(UUID uuid) { return members.contains(uuid); }
    public int size() { return members.size(); }

    public boolean isLeader(UUID uuid) { return leader.equals(uuid); }

    public boolean hasPartyChat(UUID uuid) { return partyChatActive.contains(uuid); }
    public void togglePartyChat(UUID uuid) {
        if (!partyChatActive.remove(uuid)) partyChatActive.add(uuid);
    }
}
