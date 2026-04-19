package net.tutla.pvpPlus.kit;
import org.bukkit.inventory.ItemStack;

public class Kit {
    private final String name;
    private final boolean playerMade;
    private ItemStack[] contents;    // 36 slots
    private ItemStack[] armor;       // 4 slots [boots, legs, chest, helmet]
    private ItemStack offhand;

    public Kit(String name, boolean playerMade) {
        this.name = name;
        this.playerMade = playerMade;
    }

    public String getName() { return name; }
    public boolean isPlayerMade() { return playerMade; }

    public ItemStack[] getContents() { return contents; }
    public ItemStack[] getArmor() { return armor; }
    public ItemStack getOffhand() { return offhand; }

    public void setContents(ItemStack[] contents) { this.contents = contents; }
    public void setArmor(ItemStack[] armor) { this.armor = armor; }
    public void setOffhand(ItemStack offhand) { this.offhand = offhand; }

    public boolean isEmpty() {
        return contents != null;
    }
}