package net.tutla.pvpPlus.manager;

import net.tutla.pvpPlus.kit.Kit;
import net.tutla.pvpPlus.kit.KitSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public class KitManager {

    private final Map<String, Kit> kits = new HashMap<>();
    private final KitSerializer serializer;

    public KitManager(KitSerializer serializer) {
        this.serializer = serializer;
    }

    public void loadAll() {
        serializer.loadAll().forEach(k -> kits.put(key(k.getName(), k.isPlayerMade()), k));
    }


    /**
     * Captures the player's current inventory as the kit and saves it.
     * Called by /kit capture.
     */
    public boolean createKit(String name) {
        if (kits.containsKey(key(name, false))) return false;
        Kit kit = new Kit(name, false);
        kits.put(key(name, false), kit);
        serializer.save(kit); // save empty placeholder
        return true;
    }

    public boolean saveKit(Player player, String name) {
        Kit kit = kits.get(key(name, false));
        if (kit == null) return false;

        PlayerInventory inv = player.getInventory();
        kit.setContents(cloneArray(inv.getStorageContents()));
        kit.setArmor(cloneArray(inv.getArmorContents()));
        kit.setOffhand(inv.getItemInOffHand().clone());

        serializer.save(kit);
        return true;
    }

    public boolean setIcon(Player player, String name) {
        Kit kit = kits.get(key(name, false));
        if (kit == null) return false;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) return false;
        kit.setIcon(held);
        serializer.save(kit);
        return true;
    }

    // -------------------------------------------------------------------------
    // Kit access
    // -------------------------------------------------------------------------

    public Kit getKit(String name, boolean playerMade) {
        return kits.get(key(name, playerMade));
    }

    /** Searches Server kits first, then player kits. */
    public Kit getKit(String name) {
        Kit kit = kits.get(key(name, false));
        if (kit == null) kit = kits.get(key(name, true));
        return kit;
    }

    public Collection<Kit> getAllKits() { return kits.values(); }

    public Collection<Kit> getServerKits() {
        return kits.values().stream().filter(k -> !k.isPlayerMade()).toList();
    }

    public boolean deleteKit(String name, boolean playerMade) {
        Kit removed = kits.remove(key(name, playerMade));
        if (removed != null) serializer.delete(name, playerMade);
        return removed != null;
    }

    /**
     * Applies a kit to a player — clears their inventory first.
     * This is what FightManager will call at fight start.
     */
    public void applyKit(Player player, Kit kit) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        if (kit.getContents() != null) inv.setStorageContents(cloneArray(kit.getContents()));
        if (kit.getArmor()    != null) inv.setArmorContents(cloneArray(kit.getArmor()));
        if (kit.getOffhand()  != null) inv.setItemInOffHand(kit.getOffhand().clone());
    }

    // -------------------------------------------------------------------------
    // Util
    // -------------------------------------------------------------------------

    private String key(String name, boolean playerMade) {
        return (playerMade ? "p:" : "a:") + name.toLowerCase();
    }

    private ItemStack[] cloneArray(ItemStack[] arr) {
        if (arr == null) return null;
        ItemStack[] copy = new ItemStack[arr.length];
        for (int i = 0; i < arr.length; i++) {
            copy[i] = arr[i] != null ? arr[i].clone() : null;
        }
        return copy;
    }
}