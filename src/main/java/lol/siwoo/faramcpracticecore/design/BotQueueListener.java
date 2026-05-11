package lol.siwoo.faramcpracticecore.design;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.arena.Arena;
import ga.strikepractice.battlekit.BattleKit;
import lol.siwoo.faramcpracticecore.FaraMCPracticeCore;
import lol.siwoo.faramcpracticecore.arena.ArenaSelectorGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class BotQueueListener implements Listener {

    private final FaraMCPracticeCore plugin;

    public BotQueueListener(FaraMCPracticeCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (!title.equals(PvpBotQueue.TITLE))
            return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta())
            return;

        String itemName = clickedItem.getItemMeta().getDisplayName();

        // Ignore background glass
        if (clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        String kitId = resolveKitId(itemName);
        if (kitId == null) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
            player.sendActionBar(Component.text("This kit is not available right now.").color(NamedTextColor.RED));
            return;
        }

        BattleKit kit = BattleKit.getKit(kitId);
        if (kit == null) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
            player.sendActionBar(Component.text("This kit is not available right now.").color(NamedTextColor.RED));
            return;
        }

        if (StrikePractice.getAPI().isInFight(player) || StrikePractice.getAPI().isInQueue(player)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
            player.sendActionBar(Component.text("You are already in a fight or queue!").color(NamedTextColor.RED));
            return;
        }

        // The action to start the bot fight (runs after map selection or immediately)
        Runnable startBotFight = () -> {
            try {
                // Re-check state: player may have left or entered a fight during map selection
                if (!player.isOnline() || StrikePractice.getAPI().isInFight(player))
                    return;

                Arena arena = plugin.getArenaManager().getOrAllocateDynamicArena(kit.isBuild());
                String cmd = "strikepractice:botduel " + kitId + (arena != null ? " " + arena.getName() : "");
                Bukkit.dispatchCommand(player, cmd);

                player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1, 1);
                player.sendActionBar(Component.text("Starting bot fight: " + kitId + "!").color(NamedTextColor.GREEN));
            } catch (Exception e) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
                player.sendActionBar(Component.text("Failed to start bot fight.").color(NamedTextColor.RED));
                plugin.getLogger().warning("Failed to start bot fight for " + player.getName() + ": " + e.getMessage());
            }
        };

        player.closeInventory();

        // Start bot fight directly.
        // It will trigger KitSelectEvent, which will open the map selector exactly once.
        startBotFight.run();
    }

    public static String resolveKitId(String itemName) {
        return switch (itemName) {
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
    }
}
