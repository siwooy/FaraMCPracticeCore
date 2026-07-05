package lol.siwoo.faramcpracticecore.lobby;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import ga.strikepractice.events.DuelStartEvent;
import lol.siwoo.faramcpracticecore.FaraMCPracticeCore;
import lol.siwoo.faramcpracticecore.design.MessageStyle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class KitEditor implements Listener {
    private final FaraMCPracticeCore plugin;
    private final StrikePracticeAPI api;

    public KitEditor(FaraMCPracticeCore plugin) {
        this.plugin = plugin;
        this.api = StrikePractice.getAPI();
    }

    @EventHandler
    public void onCommandExecute(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();

        if (api.isEditingKit(p)) {
            // Compare the command word exactly — startsWith("/l") also matched
            // /list, /lobby, /lp, /login, ... and force-left the kit editor
            String commandWord = e.getMessage().toLowerCase().split(" ")[0];

            if (commandWord.equals("/kiteditor")) {
                return;
            }

            if (commandWord.equals("/leave") || commandWord.equals("/l")) {
                e.setCancelled(true);
                Bukkit.dispatchCommand(p, "kiteditor leave");

                p.sendMessage(MessageStyle.info("Left Kit Editor."));
                return;
            }

            e.setCancelled(true);
            e.getPlayer().sendMessage(MessageStyle.errorWithHighlight("Leave Kit Editor first.", "/leave", ""));
        }
    }

    @EventHandler
    public void onPlayerQueue(DuelStartEvent e) {
        Player p1 = e.getPlayer1();
        Player p2 = e.getPlayer2();

        if (api.isEditingKit(p1)) {
            Bukkit.dispatchCommand(p1, "kiteditor leave");
            p1.sendMessage(MessageStyle.info("Removed from Kit Editor."));
        }

        if (api.isEditingKit(p2)) {
            Bukkit.dispatchCommand(p2, "kiteditor leave");
            p2.sendMessage(MessageStyle.info("Removed from Kit Editor."));
        }

    }
}
