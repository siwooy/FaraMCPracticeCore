package lol.siwoo.faramcpracticecore.admin;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import lol.siwoo.faramcpracticecore.design.MessageStyle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ForceWin implements CommandExecutor {

    StrikePracticeAPI api = StrikePractice.getAPI();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(MessageStyle.error("This command can only be used by a player."));
            return true;
        }

        if (p.hasPermission("faramcpracticecore.admin")) {
            if (args.length == 0) {
                if (api.getFight(p) == null) {
                    p.sendMessage(MessageStyle.error("You're not in a fight."));
                    return true;
                }
                api.forceWinFight(p);
                p.sendMessage(MessageStyle.success("Force won the fight."));
            } else if (args.length == 1) {
                // detect args type
                if (args[0].equalsIgnoreCase("fight")) {
                    if (api.getFight(p) == null) {
                        p.sendMessage(MessageStyle.error("You're not in a fight."));
                        return true;
                    }
                    api.getFight(p).getPlayersInFight().forEach(player -> {
                        player.teleport(api.getSpawnLocation());
                    });
                    api.forceWinFight(p);
                    p.sendMessage(MessageStyle.success("Force won the fight."));
                } else if (args[0].equalsIgnoreCase("round")) {
                    api.forceWinRound(p);
                    p.sendMessage(MessageStyle.success("Force won the round."));
                } else {
                    p.sendMessage(MessageStyle.error("Invalid option. Use: fight / round"));
                }
            } else {
                p.sendMessage(MessageStyle.error("Usage: /forcewin [fight/round]"));
            }
            return true;
        } else {
            p.sendMessage(MessageStyle.error("Unknown command."));
            return true;
        }
    }
}
