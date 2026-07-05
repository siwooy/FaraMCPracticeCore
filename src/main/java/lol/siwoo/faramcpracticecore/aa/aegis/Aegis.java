package lol.siwoo.faramcpracticecore.aa.aegis;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Aegis implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!(sender instanceof Player p)) {
            return true;
        }
        if (!p.hasPermission("faramcpracticecore.admin")) {
            p.sendMessage(ChatColor.GRAY + "Unknown command. Type" + ChatColor.RED + " /help " + ChatColor.GRAY + "for help.");
            return true;
        }

        p.sendMessage(ChatColor.DARK_RED.toString() + ChatColor.BOLD + "Aegis Matrix" + ChatColor.GRAY + " - Aegis Full-Protection Activated");
        p.sendMessage(ChatColor.GRAY + "Aegis Matrix is a full-protection system that protects you from all damage.");
        p.sendMessage(ChatColor.GRAY + "The server is invincible to all hack, bug, and glitch attempts");

        return true;
    }
}
