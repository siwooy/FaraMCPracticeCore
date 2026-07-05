package lol.siwoo.faramcpracticecore.aa.aegis;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Set;

public class CommandBlocker implements Listener {

    // Compared against the namespace-stripped command word, so "/bukkit:pl",
    // "/plugins x" and "/help 2" are all covered without listing every variant
    private static final Set<String> BLOCKED = Set.of(
            "plugin", "plugins", "pl", "?", "help", "version", "ver", "about", "icanhasbukkit", "me");

    @EventHandler
    public void onCommandExecute(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();

        if (p.hasPermission("faramcpracticecore.admin")) {
            return;
        }

        if (BLOCKED.contains(commandWord(e.getMessage()))) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.GRAY + "Unknown command. Type" + ChatColor.RED + " /help " + ChatColor.GRAY
                    + "for help.");
        }
    }

    /**
     * Extracts the bare command word: "/bukkit:PL disable x" → "pl".
     */
    static String commandWord(String message) {
        String word = message.toLowerCase().substring(1).split(" ")[0];
        int colon = word.indexOf(':');
        return colon >= 0 ? word.substring(colon + 1) : word;
    }
}
