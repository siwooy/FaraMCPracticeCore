package lol.siwoo.faramcpracticecore.design;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import ga.strikepractice.battlekit.BattleKit;
import ga.strikepractice.events.KitSelectEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QueueLastGame implements CommandExecutor, Listener {
    private final StrikePracticeAPI api = StrikePractice.getAPI();
    public final Map<UUID, String> lastKitData = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String [] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player p = (Player) sender;
        UUID u = p.getUniqueId();

        if (!lastKitData.containsKey(u)) {
            Bukkit.dispatchCommand(sender, "unrankedgui");
            return true;
        } else {
            String kitId = lastKitData.get(u);
            BattleKit kit = (kitId == null) ? null : BattleKit.getKit(kitId);
            if (kit == null) {
                Bukkit.dispatchCommand(sender, "unrankedgui");
                return true;
            }

            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
            api.joinQueue(p, kit);
            return true;
        }
    }

    @EventHandler
    public void onGameQueue(KitSelectEvent e) {
        Player p = e.getPlayer();
        UUID u = p.getUniqueId();

        String kitName = e.getKit() != null ? e.getKit().getName() : null;
        if (kitName == null || kitName.toLowerCase().contains("ffa")) {
            return;
        }

        lastKitData.put(u, kitName.toLowerCase(java.util.Locale.ROOT));
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent e) {
        lastKitData.remove(e.getPlayer().getUniqueId());
    }
}
