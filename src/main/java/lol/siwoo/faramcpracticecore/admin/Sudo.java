package lol.siwoo.faramcpracticecore.admin;

import lol.siwoo.faramcpracticecore.design.MessageStyle;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Sudo implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command c, String s, String[] args) {
        if (!sender.hasPermission("faramcpracticecore.admin")) {
            sender.sendMessage(MessageStyle.error("Unknown command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageStyle.error("Usage: /sudo <target> <command>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(MessageStyle.errorWithName(args[0], "is not online."));
            return true;
        }
        StringBuilder command = new StringBuilder();

        if (args.length != 2) {
            for (int i = 1; i < args.length; i++) {
                if (i != 1) {
                    command.append(" ");
                }
                command.append(args[i]);
            }
        } else {
            command = new StringBuilder(args[1]);
        }

        // Block escalation: sudo-ing another admin would let any admin run
        // commands with the privileges of a more-privileged account (e.g.
        // "/sudo owner op me"). Note dispatchCommand also bypasses the
        // command-blocker listeners, so this is the only gate.
        if (target.hasPermission("faramcpracticecore.admin") && !target.equals(sender)) {
            sender.sendMessage(MessageStyle.error("Nice try."));
            return true;
        }

        if (String.valueOf(command).startsWith("c:")) {
            target.chat(String.valueOf(command).substring(2));
            Bukkit.getServer().getLogger()
                    .info(target.getName() + " was sudoed by " + sender.getName() + " to " + command);
            return true;
        }

        Bukkit.dispatchCommand(target, String.valueOf(command));
        Bukkit.getServer().getLogger().info(
                target.getName() + " was sudoed by " + sender.getName() + " to issue server command: /" + command);
        return true;
    }
}
