package lol.siwoo.faramcpracticecore.design;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import lol.siwoo.faramcpracticecore.FaraMCPracticeCore;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelGUI implements CommandExecutor, Listener {

        private static FaraMCPracticeCore plugin;
        private static final StrikePracticeAPI api = StrikePractice.getAPI();
        public static final String TITLE_PREFIX = ChatColor.DARK_PURPLE.toString() + ChatColor.BOLD + "Duel ";

        // Store who is being dueled by whom
        public static final Map<UUID, UUID> pendingDuelTargets = new HashMap<>();

        public DuelGUI(FaraMCPracticeCore plugin) {
                DuelGUI.plugin = plugin;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (!(sender instanceof Player)) {
                        sender.sendMessage(MessageStyle.error("This command can only be used by a player."));
                        return true;
                }

                Player player = (Player) sender;

                if (args.length < 1) {
                        player.sendMessage(MessageStyle.error("Usage: /duel <player>"));
                        return true;
                }

                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || !target.isOnline()) {
                        player.sendMessage(MessageStyle.errorWithName(args[0], "is not online."));
                        return true;
                }

                if (target.equals(player)) {
                        player.sendMessage(MessageStyle.error("You can't duel yourself."));
                        return true;
                }

                if (api.isInFight(player) || api.isInQueue(player)) {
                        player.sendMessage(MessageStyle.error("You're already in a fight or queue."));
                        return true;
                }

                if (api.isInFight(target)) {
                        player.sendMessage(MessageStyle.errorWithName(target.getName(), "is currently in a fight."));
                        return true;
                }

                // Store the target and open kit selection
                pendingDuelTargets.put(player.getUniqueId(), target.getUniqueId());
                player.openInventory(createDuelGUI(player, target));
                return true;
        }

        @EventHandler
        public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent e) {
                // Entries are only removed on kit click — clean up on quit so
                // ESC-closing the GUI and logging off doesn't leak them forever
                UUID uuid = e.getPlayer().getUniqueId();
                pendingDuelTargets.remove(uuid);
                pendingDuelTargets.values().removeIf(target -> target.equals(uuid));
        }

        @EventHandler
        public void onDuelCommand(PlayerCommandPreprocessEvent e) {
                String msg = e.getMessage();
                String cmd = msg.split(" ")[0].toLowerCase();
                if (cmd.equals("/duel") || cmd.equals("/strikepractice:duel")) {
                        e.setCancelled(true);
                        // Extract args and call our executor directly — never let SP handle it
                        String[] parts = msg.split(" ", 2);
                        String[] args = parts.length > 1 ? parts[1].split(" ") : new String[0];
                        onCommand(e.getPlayer(), null, "duel", args);
                }
        }

        public static Inventory createDuelGUI(Player p, Player target) {
                Inventory gui = Bukkit.createInventory(null, 45, TITLE_PREFIX + target.getName());

                fillBackground(gui);

                addQueueItem(gui, 10, Material.SOUL_SAND, "WindFight",
                                ChatColor.AQUA.toString() + ChatColor.BOLD + "WindFight", p);
                addQueueItem(gui, 11, Material.DIAMOND_SWORD, "Sword",
                                ChatColor.AQUA + "Sword", p);
                addQueueItem(gui, 12, Material.DIAMOND_AXE, "Axe",
                                ChatColor.AQUA + "Axe", p);
                addQueueItem(gui, 13, Material.DIAMOND_CHESTPLATE, "Boxing",
                                ChatColor.AQUA + "Boxing", p);
                addQueueItem(gui, 14, createNodebuffPotion(), "Nodebuff",
                                ChatColor.LIGHT_PURPLE + "Nodebuff", p);
                addQueueItem(gui, 15, Material.LAVA_BUCKET, "BuildUHC",
                                ChatColor.YELLOW + "BuildUHC", p);
                addQueueItem(gui, 16, Material.LEAD, "Sumo",
                                ChatColor.GOLD + "Sumo", p);
                addQueueItem(gui, 19, Material.PUFFERFISH, "Combo",
                                ChatColor.RED + "Combo", p);
                addQueueItem(gui, 20, Material.GOLDEN_APPLE, "Gapple",
                                ChatColor.GOLD + "Gapple", p);
                addQueueItem(gui, 21, Material.RED_BED, "BedFight",
                                ChatColor.RED + "BedFight", p);
                addQueueItem(gui, 22, Material.FIRE_CHARGE, "Fireball Fight",
                                ChatColor.RED + "Fireball Fight", p);
                addQueueItem(gui, 23, Material.ENDER_EYE, "SkyWars",
                                ChatColor.AQUA + "SkyWars", p);
                addQueueItem(gui, 24, Material.BOW, "Archer",
                                ChatColor.YELLOW + "Archer", p);
                addQueueItem(gui, 25, Material.IRON_SWORD, "No Enchant",
                                ChatColor.YELLOW + "No Enchant", p);
                addQueueItem(gui, 28, Material.IRON_SHOVEL, "Spleef",
                                ChatColor.YELLOW + "Spleef", p);
                addQueueItem(gui, 29, Material.WOODEN_SWORD, "SG",
                                ChatColor.RED + "SG", p);
                addQueueItem(gui, 30, Material.MUSHROOM_STEW, "Soup",
                                ChatColor.YELLOW + "Soup", p);
                addQueueItem(gui, 31, Material.NAME_TAG, "Combo Tag",
                                ChatColor.YELLOW + "Combo Tag", p);

                return gui;
        }

        private static void fillBackground(Inventory gui) {
                ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta glassMeta = glass.getItemMeta();
                glassMeta.setDisplayName(" ");
                glass.setItemMeta(glassMeta);

                for (int i = 0; i < gui.getSize(); i++) {
                        if (gui.getItem(i) == null)
                                gui.setItem(i, glass);
                }
        }

        private static ItemStack createNodebuffPotion() {
                ItemStack potion = new ItemStack(Material.SPLASH_POTION);
                PotionMeta meta = (PotionMeta) potion.getItemMeta();
                if (meta != null) {
                        meta.setBasePotionType(PotionType.REGENERATION);
                        potion.setItemMeta(meta);
                }
                return potion;
        }

        private static void addQueueItem(Inventory gui, int slot, Material material, String gameMode,
                        String displayName, Player p) {
                addQueueItem(gui, slot, new ItemStack(material), gameMode, displayName, p);
        }

        private static void addQueueItem(Inventory gui, int slot, ItemStack item, String gameMode,
                        String displayName, Player p) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(displayName);

                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS,
                                ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ARMOR_TRIM,
                                ItemFlag.HIDE_DYE, ItemFlag.HIDE_STORED_ENCHANTS);

                meta.setLore(Arrays.asList(
                                "",
                                ChatColor.GREEN + "Click to send duel request!"));

                item.setItemMeta(meta);
                gui.setItem(slot, item);
        }
}
