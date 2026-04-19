package net.tutla.pvpPlus.kit;
import org.bukkit.inventory.ItemStack;

public class Kit {
    private final String name;private ItemStack icon;
    private final boolean playerMade;
    private ItemStack[] contents;    // 36 slots
    private ItemStack[] armor;       // 4 slots [boots, legs, chest, helmet]
    private ItemStack offhand;

    public Kit(String name, boolean playerMade) {
        this.name = name;
        this.playerMade = playerMade;
    }

    public String getName() { return name; }
    public ItemStack getIcon() { return icon; }
    public boolean isPlayerMade() { return playerMade; }

    public ItemStack[] getContents() { return contents; }
    public ItemStack[] getArmor() { return armor; }
    public ItemStack getOffhand() { return offhand; }


    public void setContents(ItemStack[] contents) { this.contents = contents; }
    public void setIcon(ItemStack icon) { this.icon = icon.clone(); }
    public void setArmor(ItemStack[] armor) { this.armor = armor; }
    public void setOffhand(ItemStack offhand) { this.offhand = offhand; }


    public boolean hasIcon() {
        return icon != null;
    }
    public boolean isEmpty() {
        return contents != null;
    }
}