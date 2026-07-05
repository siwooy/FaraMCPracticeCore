package lol.siwoo.faramcpracticecore.design;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;

public class RankedQueue implements CommandExecutor, Listener {

    private static final String GUI_TITLE = ChatColor.DARK_RED.toString() + ChatColor.BOLD + "Ranked: COMING SOON";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageStyle.error("This command can only be used by a player."));
            return true;
        }

        Player player = (Player) sender;
        player.openInventory(createQueueGUI(player, 0, "placeholder"));

        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Without this, players can drag the background panes out of the GUI
        // (free items) or shift-click their own gear into it and lose it
        if (event.getView().getTitle().equals(GUI_TITLE)) {
            event.setCancelled(true);
        }
    }

    public static Inventory createQueueGUI(Player player, int slot, String name) {
        // Inventory gui = Bukkit.createInventory(null, 45, ChatColor.DARK_PURPLE +
        // "Queue Selection");
        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);

        // Fill background
        fillBackground(gui);

        if (slot != 0) {
            ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName(ChatColor.GOLD + "Queued for " + name);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "You are currently Queued for the " + name + " Queue",
                    "",
                    ChatColor.RED + "Click Again to Leave the Queue!"));

            item.setItemMeta(meta);
            gui.setItem(slot, item);
        }

        return gui;
    }

    private static void fillBackground(Inventory gui) {
        ItemStack glass = new ItemStack(Material.GLASS_PANE, 1, (short) 7);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        // Fill empty slots
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, glass);
            }
        }
    }

    public static ItemStack createRegenerationPotion() {
        ItemStack regenerationPotion = new ItemStack(Material.POTION);

        PotionMeta potionMeta = (PotionMeta) regenerationPotion.getItemMeta();

        PotionEffect regenerationEffect = new PotionEffect(
                PotionEffectType.REGENERATION,
                10 * 20,
                10);

        potionMeta.addCustomEffect(regenerationEffect, true);
        regenerationPotion.setItemMeta(potionMeta);
        return regenerationPotion;
    }

    private static void addQueueItem(Inventory gui, int slot, Material material, String gameMode,
            String displayName, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(displayName);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + description,
                "",
                ChatColor.GREEN + "Click to join!"));

        item.setItemMeta(meta);
        gui.setItem(slot, item);
    }
}