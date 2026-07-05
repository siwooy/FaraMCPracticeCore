package lol.siwoo.faramcpracticecore.aa.aicoach;

import ga.strikepractice.api.StrikePracticeAPI;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class AICoach implements CommandExecutor {
    private final Set<UUID> aiCoachEnabled = new HashSet<>();
    private final Map<UUID, BukkitRunnable> activeMonitors = new HashMap<>();
    private final Plugin plugin;
    private final StrikePracticeAPI api;

    public AICoach(Plugin plugin, StrikePracticeAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        if (aiCoachEnabled.contains(playerUUID)) {
            aiCoachEnabled.remove(playerUUID);
            stopMonitoring(playerUUID);
            player.sendMessage(ChatColor.RED + "✨ AI Coach Disabled!");
        } else {
            aiCoachEnabled.add(playerUUID);
            player.sendMessage(ChatColor.GREEN + "✨ AI Coach Enabled!");
        }

        return true;
    }

    public void startMonitoring(final Player player, final Player opponent) {
        if (!aiCoachEnabled.contains(player.getUniqueId())) {
            return;
        }

        final StrikePracticeAPI apiRef = this.api;

        BukkitRunnable monitor = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if either player is offline or the fight has ended
                if (!player.isOnline() || !opponent.isOnline()) {
                    stopMonitoring(player.getUniqueId());
                    return;
                }

                // Check if the fight is still ongoing using StrikePractice API
                // (getFight returns null once the fight is removed — an NPE here
                // would repeat every 10 ticks since Bukkit doesn't cancel
                // repeating tasks on exception)
                if (apiRef != null
                        && (apiRef.getFight(player) == null || apiRef.getFight(player).hasEnded())) {
                    stopMonitoring(player.getUniqueId());
                    return;
                }

                Location playerLoc = player.getLocation();
                Location oppLoc = opponent.getLocation();
                Vector aimDirection = player.getLocation().getDirection();
                // Fixed: Using proper method to check if player is blocking
                boolean isBlocking = player.isBlocking();

                // Calculate distance between players
                double distance = playerLoc.distance(oppLoc);

                // Format the data with extra newlines for better visibility
                String data = String.format(
                    "\n§6=== Combat Data ===\n" +
                    "§fYour Position: §a%.1f, %.1f, %.1f\n" +
                    "§fOpponent Position: §c%.1f, %.1f, %.1f\n" +
                    "§fDistance to opponent: §e%.1f blocks\n" +
                    "§fAim Direction: §e%.1f, %.1f, %.1f\n" +
                    "§fBlocking: §b%s\n",
                    playerLoc.getX(), playerLoc.getY(), playerLoc.getZ(),
                    oppLoc.getX(), oppLoc.getY(), oppLoc.getZ(),
                    distance,
                    aimDirection.getX(), aimDirection.getY(), aimDirection.getZ(),
                    isBlocking ? "Yes" : "No"
                );

                player.sendMessage(data);
            }
        };

        // Cancel any previous monitor for this player — overwriting the map
        // entry would orphan the old task and leave it running forever
        stopMonitoring(player.getUniqueId());

        monitor.runTaskTimer(plugin, 0L, 10L); // 10 ticks = 0.5 seconds
        activeMonitors.put(player.getUniqueId(), monitor);
    }

    public void stopMonitoring(UUID playerUUID) {
        BukkitRunnable monitor = activeMonitors.remove(playerUUID);
        if (monitor != null) {
            monitor.cancel();
        }
    }

    public boolean hasAICoach(UUID playerUUID) {
        return aiCoachEnabled.contains(playerUUID);
    }
}