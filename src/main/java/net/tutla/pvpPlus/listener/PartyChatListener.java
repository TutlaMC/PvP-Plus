package net.tutla.pvpPlus.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.tutla.pvpPlus.party.Party;
import net.tutla.pvpPlus.party.PartyManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PartyChatListener implements Listener {

    private final PartyManager partyManager;

    public PartyChatListener(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        if (!partyManager.hasPartyChatActive(event.getPlayer())) return;

        event.setCancelled(true); // stop it going to global chat

        Party party = partyManager.getParty(event.getPlayer());
        String plainText = PlainTextComponentSerializer.plainText().serialize(event.message());
        partyManager.broadcastToParty(party,
                "<white>" + event.getPlayer().getName() + ": <reset>" + plainText);
    }
}