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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static lol.siwoo.faramcpracticecore.design.QueueGUIListener.updateInventory;

public class UnrankedGUI implements CommandExecutor, Listener {

        private static FaraMCPracticeCore plugin;
        // One live update timer per player — createQueueGUI is called again on
        // every queue join/leave while the GUI stays open, and without this the
        // timers stacked unboundedly (each doing 36 placeholder lookups/second)
        private static final Map<UUID, BukkitRunnable> updateTasks = new HashMap<>();
        StrikePracticeAPI api = StrikePractice.getAPI();

        public UnrankedGUI(FaraMCPracticeCore plugin) {
                this.plugin = plugin;
        }

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

        public static Inventory createQueueGUI(Player p, int slot, String name) {
                Inventory gui = Bukkit.createInventory(null, 45,
                                ChatColor.YELLOW.toString() + ChatColor.BOLD + "Unranked Queue");

                // Fill background
                fillBackground(gui);

                // Add queue items
                addQueueItem(gui, 10, Material.SOUL_SAND, "WindFight",
                                ChatColor.AQUA.toString() + ChatColor.BOLD + "WindFight",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA
                                                + "%strikepractice_in_queue_count_windfight%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA
                                                + "%strikepractice_in_fight_count_windfight%",
                                p);
                addQueueItem(gui, 11, Material.DIAMOND_SWORD, "Sword",
                                ChatColor.AQUA + "Sword",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA + "%strikepractice_in_queue_count_sword%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA + "%strikepractice_in_fight_count_sword%",
                                p);
                addQueueItem(gui, 12, Material.DIAMOND_AXE, "Axe",
                                ChatColor.AQUA + "Axe",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA + "%strikepractice_in_queue_count_axepvp%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA
                                                + "%strikepractice_in_fight_count_axepvp%",
                                p);
                addQueueItem(gui, 13, Material.DIAMOND_CHESTPLATE, "Boxing",
                                ChatColor.AQUA + "Boxing",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA + "%strikepractice_in_queue_count_boxing%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA
                                                + "%strikepractice_in_fight_count_boxing%",
                                p);
                addQueueItem(gui, 14, createNodebuffPotion(), "Nodebuff",
                                ChatColor.LIGHT_PURPLE + "Nodebuff",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA
                                                + "%strikepractice_in_queue_count_nodebuff%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA
                                                + "%strikepractice_in_fight_count_nodebuff%",
                                p);
                addQueueItem(gui, 15, Material.LAVA_BUCKET, "BuildUHC",
                                ChatColor.YELLOW + "BuildUHC",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA
                                                + "%strikepractice_in_queue_count_builduhc%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA
                                                + "%strikepractice_in_fight_count_builduhc%",
                                p);
                addQueueItem(gui, 16, Material.LEAD, "Sumo",
                                ChatColor.GOLD + "Sumo",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA + "%strikepractice_in_queue_count_sumo%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA + "%strikepractice_in_fight_count_sumo%",
                                p);
                addQueueItem(gui, 19, Material.PUFFERFISH, "Combo",
                                ChatColor.RED + "Combo",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA + "%strikepractice_in_queue_count_combo%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA + "%strikepractice_in_fight_count_combo%",
                                p);
                addQueueItem(gui, 20, Material.GOLDEN_APPLE, "Gapple",
                                ChatColor.GOLD + "Gapple",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA + "%strikepractice_in_queue_count_gapple%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA
                                                + "%strikepractice_in_fight_count_gapple%",
                                p);
                addQueueItem(gui, 21, Material.RED_BED, "BedFight",
                                ChatColor.RED + "BedFight",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA
                                                + "%strikepractice_in_queue_count_bedfight%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA
                                                + "%strikepractice_in_fight_count_bedfight%",
                                p);
                addQueueItem(gui, 22, Material.FIRE_CHARGE, "Fireball Fight",
                                ChatColor.RED + "Fireball Fight",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA
                                                + "%strikepractice_in_queue_count_fireballfight%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA
                                                + "%strikepractice_in_fight_count_fireballfight%",
                                p);
                addQueueItem(gui, 23, Material.ENDER_EYE, "SkyWars",
                                ChatColor.AQUA + "SkyWars",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA
                                                + "%strikepractice_in_queue_count_skywars%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA
                                                + "%strikepractice_in_fight_count_skywars%",
                                p);
                addQueueItem(gui, 24, Material.BOW, "Archer",
                                ChatColor.YELLOW + "Archer",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA + "%strikepractice_in_queue_count_archer%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA
                                                + "%strikepractice_in_fight_count_archer%",
                                p);
                addQueueItem(gui, 25, Material.IRON_SWORD, "No Enchant",
                                ChatColor.YELLOW + "No Enchant",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA
                                                + "%strikepractice_in_queue_count_noenchant%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA
                                                + "%strikepractice_in_fight_count_noenchant%",
                                p);
                addQueueItem(gui, 28, Material.IRON_SHOVEL, "Spleef",
                                ChatColor.YELLOW + "Spleef",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA + "%strikepractice_in_queue_count_spleef%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA
                                                + "%strikepractice_in_fight_count_spleef%",
                                p);
                addQueueItem(gui, 29, Material.WOODEN_SWORD, "SG",
                                ChatColor.RED + "SG",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA + "%strikepractice_in_queue_count_sg%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA + "%strikepractice_in_fight_count_sg%",
                                p);
                addQueueItem(gui, 30, Material.MUSHROOM_STEW, "Soup",
                                ChatColor.YELLOW + "Soup",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA + "%strikepractice_in_queue_count_soup%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA + "%strikepractice_in_fight_count_soup%",
                                p);
                addQueueItem(gui, 31, Material.NAME_TAG, "Combo Tag",
                                ChatColor.YELLOW + "Combo Tag",
                                ChatColor.GRAY + "Queued: " + ChatColor.AQUA
                                                + "%strikepractice_in_queue_count_combotag%",
                                ChatColor.GRAY + "Playing: " + ChatColor.AQUA
                                                + "%strikepractice_in_fight_count_combotag%",
                                p);

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

                BukkitRunnable previous = updateTasks.remove(p.getUniqueId());
                if (previous != null) {
                        previous.cancel();
                }

                BukkitRunnable updateTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                                if (!p.isOnline() || !(Objects.equals(p.getOpenInventory().getTitle(),
                                                ChatColor.YELLOW.toString() + ChatColor.BOLD + "Unranked Queue"))) {
                                        updateTasks.remove(p.getUniqueId(), this);
                                        this.cancel();
                                        return;
                                }
                                updateInventory(p, p.getOpenInventory().getTopInventory());
                        }
                };
                updateTasks.put(p.getUniqueId(), updateTask);
                updateTask.runTaskTimer(plugin, 20L, 20L);

                return gui;
        }

        private static void fillBackground(Inventory gui) {
                ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1, (short) 7);
                ItemMeta glassMeta = glass.getItemMeta();
                glassMeta.setDisplayName(" ");
                glass.setItemMeta(glassMeta);

                for (int i = 0; i < gui.getSize(); i++) {
                        if (gui.getItem(i) == null) {
                                gui.setItem(i, glass);
                        }
                }
        }

        public static ItemStack createNodebuffPotion() {
                ItemStack potion = new ItemStack(Material.SPLASH_POTION, 1);
                PotionMeta meta = (PotionMeta) potion.getItemMeta();

                if (meta != null) {
                        meta.setBasePotionType(PotionType.REGENERATION);
                        potion.setItemMeta(meta);
                }

                return potion;
        }

        private static void addQueueItem(Inventory gui, int slot, Material material, String gameMode,
                        String displayName, String queued, String playing, Player p) {
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();

                meta.setDisplayName(displayName);
                queued = PlaceholderAPI.setPlaceholders(p, queued);
                playing = PlaceholderAPI.setPlaceholders(p, playing);

                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
                meta.addItemFlags(ItemFlag.HIDE_DYE);
                meta.addItemFlags(ItemFlag.HIDE_STORED_ENCHANTS);

                meta.setLore(Arrays.asList(
                                queued,
                                playing,
                                "",
                                ChatColor.GREEN + "Click to join!"));

                item.setItemMeta(meta);
                gui.setItem(slot, item);
        }

        private static void addQueueItem(Inventory gui, int slot, ItemStack item, String gameMode,
                        String displayName, String queued, String playing, Player p) {
                ItemMeta meta = item.getItemMeta();

                meta.setDisplayName(displayName);
                queued = PlaceholderAPI.setPlaceholders(p, queued);
                playing = PlaceholderAPI.setPlaceholders(p, playing);

                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
                meta.addItemFlags(ItemFlag.HIDE_DYE);
                meta.addItemFlags(ItemFlag.HIDE_STORED_ENCHANTS);

                meta.setLore(Arrays.asList(
                                queued,
                                playing,
                                "",
                                ChatColor.GREEN + "Click to join!"));

                item.setItemMeta(meta);
                gui.setItem(slot, item);
        }

        @EventHandler
        public void onQueueCommand(PlayerCommandPreprocessEvent e) {
                Player p = e.getPlayer();

                if (e.getMessage().equalsIgnoreCase("/queue")
                                || e.getMessage().equalsIgnoreCase("/strikepractice:queue")
                                || e.getMessage().toLowerCase().startsWith("/unranked")
                                || e.getMessage().toLowerCase().startsWith("/strikepractice:unranked")) {
                        e.setCancelled(true);
                        Bukkit.dispatchCommand(p, "unrankedgui");
                }
        }
}