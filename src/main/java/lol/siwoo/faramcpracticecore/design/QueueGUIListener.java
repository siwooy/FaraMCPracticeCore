package lol.siwoo.faramcpracticecore.design;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import ga.strikepractice.battlekit.BattleKit;
import ga.strikepractice.events.DuelStartEvent;
import ga.strikepractice.events.FightStartEvent;
import lol.siwoo.faramcpracticecore.FaraMCPracticeCore;
import lol.siwoo.faramcpracticecore.arena.ArenaSelectorGUI; // Added import
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class QueueGUIListener implements Listener {

    private final FaraMCPracticeCore plugin; // Changed to instance variable
    private final StrikePracticeAPI api = StrikePractice.getAPI();

    public QueueGUIListener(FaraMCPracticeCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (!title.equals(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Unranked Queue"))
            return;

        event.setCancelled(true);

        // Only react to clicks in the GUI itself, not the player's own
        // inventory (a renamed item in the hotbar could match a kit name, and
        // bottom-slot indexes would corrupt GUI slots in afterActivities)
        if (event.getClickedInventory() != event.getView().getTopInventory())
            return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta())
            return;

        String itemName = clickedItem.getItemMeta().getDisplayName();

        if (clickedItem.getType().equals(Material.REDSTONE_BLOCK)) {
            Bukkit.dispatchCommand(player, "queue leave");
            newafterActivities(event);
            return;
        }

        String kitId = switch (itemName) {
            case String s when s.contains("WindFight") -> "windfight";
            case String s when s.contains("Sword") -> "sword";
            case String s when s.contains("Axe") -> "axepvp";
            case String s when s.contains("Boxing") -> "boxing";
            case String s when s.contains("Nodebuff") -> "nodebuff";
            case String s when s.contains("BuildUHC") -> "builduhc";
            case String s when s.contains("Sumo") -> "sumo";
            case String s when s.contains("Combo Tag") -> "combotag";
            case String s when s.contains("Combo") -> "combo";
            case String s when s.contains("Gapple") -> "gapple";
            case String s when s.contains("BedFight") -> "bedfight";
            case String s when s.contains("Fireball Fight") -> "fireballfight";
            case String s when s.contains("SkyWars") -> "skywars";
            case String s when s.contains("Archer") -> "archer";
            case String s when s.contains("No Enchant") -> "noenchant";
            case String s when s.contains("Spleef") -> "spleef";
            case String s when s.contains("SG") -> "sg";
            case String s when s.contains("Soup") -> "soup";
            default -> null;
        };

        if (kitId != null) {
            BattleKit kit = BattleKit.getKit(kitId);
            if (kit == null) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
                player.sendActionBar(Component.text("This kit is not available right now. Try again Later.")
                        .color(NamedTextColor.RED));
                return;
            }

            // FIXED: Proper null check for ArenaManager and kit selection
            if (plugin.getArenaManager().getRandomArenaForKit(kitId) == null) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
                player.sendActionBar(
                        Component.text("No arenas available for this gamemode right now.").color(NamedTextColor.RED));
                return;
            }

            // The action to join queue (runs after map selection or immediately)
            final String finalKitId = kitId;
            Runnable joinQueue = () -> {
                try {
                    if (!player.isOnline())
                        return;
                    api.joinQueue(player, kit);
                    // Reopen queue GUI with the queued state
                    player.openInventory(UnrankedGUI.createQueueGUI(player, event.getSlot(),
                            event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()
                                    ? event.getCurrentItem().getItemMeta().getDisplayName()
                                    : finalKitId));
                    player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1, 1);
                    player.sendActionBar(
                            Component.text("Joined " + finalKitId + " queue!").color(NamedTextColor.GREEN));
                } catch (Exception e) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
                    player.sendActionBar(
                            Component.text("Failed to join queue. Try again later.").color(NamedTextColor.RED));
                    plugin.getLogger()
                            .warning("Failed to add player " + player.getName() + " to queue for kit " + finalKitId
                                    + ": " + e.getMessage());
                }
            };

            // If permissioned: open map selector first, queue fires on callback
            if (player.hasPermission("faramcpracticecore.selectarena")) {
                player.closeInventory();
                ArenaSelectorGUI.open(player, plugin.getArenaManager(), kitId, joinQueue);
            } else {
                try {
                    api.joinQueue(player, kit);
                    afterActivities(event);
                    player.sendActionBar(Component.text("Joined " + kitId + " queue!").color(NamedTextColor.GREEN));
                } catch (Exception e) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
                    player.sendActionBar(
                            Component.text("Failed to join queue. Try again later.").color(NamedTextColor.RED));
                    plugin.getLogger().warning("Failed to add player " + player.getName() + " to queue for kit " + kitId
                            + ": " + e.getMessage());
                }
            }
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
            player.sendActionBar(
                    Component.text("This kit is not available right now. Try again Later.").color(NamedTextColor.RED));
        }
    }

    public void afterActivities(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        p.playSound(p.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1, 1);

        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null)
            return;

        ItemStack leaveItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta leaveMeta = leaveItem.getItemMeta();
        String originalName = clickedItem.getItemMeta().getDisplayName();

        leaveMeta.setDisplayName(ChatColor.GOLD + "Queued for " + originalName);
        leaveMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "You are currently Queued for the " + originalName + " Queue",
                "",
                ChatColor.RED + "Click Again to Leave the Queue!"));
        leaveItem.setItemMeta(leaveMeta);
        e.getInventory().setItem(e.getSlot(), leaveItem);
    }

    public static void updateInventory(Player p, Inventory i) {
        String[] kits = { "windfight", "sword", "axepvp", "boxing", "nodebuff", "builduhc", "sumo", "combo", "gapple",
                "bedfight", "fireballfight", "skywars", "archer", "noenchant", "spleef", "sg", "soup", "combotag" };
        int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31 };

        for (int k = 0; k < kits.length; k++) {
            updateQueueItem(p, i, slots[k],
                    ChatColor.GRAY + "Queued: " + ChatColor.AQUA + "%strikepractice_in_queue_count_" + kits[k] + "%",
                    ChatColor.GRAY + "Playing: " + ChatColor.AQUA + "%strikepractice_in_fight_count_" + kits[k] + "%");
        }
    }

    public static void updateQueueItem(Player p, Inventory gui, int slot, String queued, String playing) {
        ItemStack item = gui.getItem(slot);
        if (item == null)
            return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        if (item.getType() == Material.REDSTONE_BLOCK) {
            String displayName = meta.getDisplayName();
            int forIndex = displayName.indexOf(" for ");
            if (forIndex != -1) {
                String name = displayName.substring(forIndex + 5);
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "You are currently Queued for the " + name + " Queue",
                        "",
                        PlaceholderAPI.setPlaceholders(p, queued),
                        PlaceholderAPI.setPlaceholders(p, playing),
                        "",
                        ChatColor.RED + "Click Again to Leave the Queue!"));
            }
        } else {
            meta.setLore(Arrays.asList(
                    PlaceholderAPI.setPlaceholders(p, queued),
                    PlaceholderAPI.setPlaceholders(p, playing),
                    "",
                    ChatColor.GREEN + "Click to join!"));
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ARMOR_TRIM, ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_STORED_ENCHANTS);
        item.setItemMeta(meta);
    }

    public void newafterActivities(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        p.playSound(p.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1, 1);
        p.openInventory(UnrankedGUI.createQueueGUI(p, 0, "placeholder"));
    }

    @EventHandler
    public void onFightStart(FightStartEvent e) {
        e.getFight().getPlayersInFight().forEach(Player::closeInventory);
    }

    @EventHandler
    public void onDuelStart(DuelStartEvent e) {
        e.getFight().getPlayersInFight().forEach(Player::closeInventory);
    }
}