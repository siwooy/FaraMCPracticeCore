package lol.siwoo.faramcpracticecore.aa.terms;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class JoinMessage implements Listener {

    private static JavaPlugin plugin;
    private static AgreementUtils agreementUtils;

    public static void initialize(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        agreementUtils = new AgreementUtils(plugin);
    }

    public static void sendJoinMessage(Player p) {
        if (agreementUtils != null && agreementUtils.hasPlayerAgreed(p)) {
            return;
        }

        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 2, 3);
        new BukkitRunnable() {
            @Override
            public void run() {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 2, 3);
            }
        }.runTaskLater(plugin, 8L);
        new BukkitRunnable() {
            @Override
            public void run() {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 2, 1);
            }
        }.runTaskLater(plugin, 16L);
        new BukkitRunnable() {
            @Override
            public void run() {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 2, 3);
            }
        }.runTaskLater(plugin, 24L);

        new BukkitRunnable() {
            @Override
            public void run() {
                p.sendMessage("\n");
                p.sendMessage(
                        ChatColor.BOLD.toString() + ChatColor.LIGHT_PURPLE + "Welcome to FaraMC Practice Beta!\n" +
                                ChatColor.GRAY + "Before you can start playing, you need to agree to our Beta Test and Data Analytics Agreement.\n" +
                                ChatColor.YELLOW + "Please read the agreement below carefully and click 'Agree' to continue.\n" + ChatColor.DARK_GRAY + "\n");
                p.sendMessage("\n");
                p.sendMessage(
                        ChatColor.BOLD.toString() + ChatColor.DARK_PURPLE + "Beta Test and Data Analytics Agreement\n" +
                                ChatColor.GRAY + "By accepting this agreement, you consent to provide FaraMC (operating under the online names \"siwoo,\" \"velocated,\" and \"worldy\" at faramc.uk) with access to collect gameplay data during our beta testing period.\n" +
                                "1. Data Collection: You grant FaraMC permission to collect and analyze gameplay analytics and performance data (\"Data\") resulting from your use of the software.\n" +
                                "2. Use of Data: FaraMC will use this Data for the purpose of product improvement, which includes, but is not limited to, enhancing the player experience, identifying and fixing bugs, and training specific data models.\n" +
                                "3. Disclaimer of Warranty & Limitation of Liability: The beta software is provided on an \"as-is\" and \"as-available\" basis. By participating, you understand and agree that FaraMC shall not be liable for any direct, indirect, incidental, or consequential damages arising from your participation in the beta program or from the collection and use of Data as outlined in this agreement. You hereby waive any right to bring a claim or lawsuit against FaraMC for such damages.\n" +
                                "By proceeding, you confirm that you have read, understood, and agree to be bound by all statements in this agreement.");

                TextComponent agree = new TextComponent(ChatColor.BOLD.toString() + ChatColor.GREEN + "Agree | ");
                agree.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{
                        new TextComponent(ChatColor.GRAY + "By Clicking This,\nYou confirm that you have read, understood, and agree\nto be bound by all statements in this agreement.")
                }));
                agree.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/terms_agree"));
                TextComponent disagree = new TextComponent(ChatColor.RED + "Disagree");
                disagree.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{
                        new TextComponent(ChatColor.RED + "You have to agree in to continue playing\n" + ChatColor.RED + "the beta version of the server.")
                }));
                disagree.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/terms_disagree"));

                p.spigot().sendMessage(agree, disagree);
                p.sendMessage("\n");
            }
        }.runTaskLater(plugin, 25L);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String command = e.getMessage().toLowerCase();

        if (command.startsWith("/terms_agree") || command.startsWith("/terms_disagree")) {
            if (agreementUtils != null && agreementUtils.hasPlayerAgreed(p)) {
                p.sendMessage(ChatColor.GREEN + "You have already agreed to the terms and conditions.");
                e.setCancelled(true);
                return;
            }

            return;
        }

        if (agreementUtils != null && !agreementUtils.hasPlayerAgreed(p)) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "You must agree to the terms and conditions first before playing the beta version of the server.");
        }
    }

    @EventHandler
    public void onGuiOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) {
            return;
        }

        if (agreementUtils != null && !agreementUtils.hasPlayerAgreed(p)) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "You must agree to the terms and conditions first before playing the beta version of the server.");
        }
    }
}
