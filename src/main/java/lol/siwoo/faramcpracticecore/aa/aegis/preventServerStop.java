package lol.siwoo.faramcpracticecore.aa.aegis;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Set;

public class preventServerStop implements Listener {

    private static final Set<String> BLOCKED = Set.of("stop", "reload", "rl", "restart");
    private static final Set<String> PLUGIN_COMMANDS = Set.of("plugin", "plugins", "pl");
    private static final Set<String> PROTECTED_PLUGINS = Set.of("faramcpracticecore", "strikepractice");

    @EventHandler
    public void onCommandExecute(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String message = e.getMessage().toLowerCase();
        // Token-based matching: the old exact-string list was bypassed by a
        // trailing space, an argument ("/reload confirm"), or an unlisted
        // namespace prefix ("/plugin disable strikepractice")
        String word = CommandBlocker.commandWord(message);

        boolean blocked = BLOCKED.contains(word);

        if (!blocked && PLUGIN_COMMANDS.contains(word) && message.contains("disable")) {
            for (String protectedPlugin : PROTECTED_PLUGINS) {
                if (message.contains(protectedPlugin)) {
                    blocked = true;
                    break;
                }
            }
        }

        if (blocked) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.GRAY + "Unknown command. Type" + ChatColor.RED + " /help " + ChatColor.GRAY
                    + "for help.");
        }
    }
}
