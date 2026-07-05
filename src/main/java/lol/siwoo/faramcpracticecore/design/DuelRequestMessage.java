package lol.siwoo.faramcpracticecore.design;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import ga.strikepractice.fights.duel.Duel;
import ga.strikepractice.fights.requests.DuelRequest;
import ga.strikepractice.fights.requests.FightRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DuelRequestMessage implements Listener, CommandExecutor {

    private static final Component SEPARATOR = Component.text("                                    ")
            .color(NamedTextColor.DARK_GRAY).decorate(TextDecoration.STRIKETHROUGH);

    // The SP API exposes no way to remove a DuelRequest, so a declined request
    // stays "live" until it expires. Track declines ourselves so Accept after
    // Decline doesn't start the duel anyway. Keyed by target UUID → lowercase
    // sender names.
    private static final Map<UUID, Set<String>> declinedRequests = new HashMap<>();

    /**
     * Sends an apple-styled duel request message to both sender and target.
     */
    public static void sendDuelRequestMessage(Player sender, Player target, String kitName, String mapName) {
        // A fresh request supersedes an earlier decline of the same pair
        Set<String> declined = declinedRequests.get(target.getUniqueId());
        if (declined != null) {
            declined.remove(sender.getName().toLowerCase());
        }
        String displayMap = (mapName != null && !mapName.isEmpty()) ? mapName : "Random";

        // ── Message to the SENDER ──
        Component senderMsg = Component.empty()
                .append(Component.newline())
                .append(SEPARATOR)
                .append(Component.newline())
                .append(Component.text("  ⚔ ", NamedTextColor.GOLD))
                .append(Component.text("Duel Request Sent", NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("  To: ", NamedTextColor.GRAY))
                .append(Component.text(target.getName(), NamedTextColor.AQUA))
                .append(Component.newline())
                .append(Component.text("  Kit: ", NamedTextColor.GRAY))
                .append(Component.text(kitName, NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("  Map: ", NamedTextColor.GRAY))
                .append(Component.text(displayMap, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("  Waiting for response...", NamedTextColor.DARK_GRAY)
                        .decorate(TextDecoration.ITALIC))
                .append(Component.newline())
                .append(SEPARATOR)
                .append(Component.newline());

        sender.sendMessage(senderMsg);

        // ── Message to the TARGET ──
        Component acceptBtn = Component.text(" ✓ Accept ", NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/duelaccept " + sender.getName()))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Click to accept this duel", NamedTextColor.GREEN)));

        Component declineBtn = Component.text(" ✗ Decline ", NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/dueldecline " + sender.getName()))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Click to decline this duel", NamedTextColor.RED)));

        // Wrap buttons in colored brackets
        Component acceptWrapped = Component.empty()
                .append(Component.text("  [", NamedTextColor.GREEN))
                .append(acceptBtn)
                .append(Component.text("]", NamedTextColor.GREEN));

        Component declineWrapped = Component.empty()
                .append(Component.text("  [", NamedTextColor.RED))
                .append(declineBtn)
                .append(Component.text("]", NamedTextColor.RED));

        Component targetMsg = Component.empty()
                .append(Component.newline())
                .append(SEPARATOR)
                .append(Component.newline())
                .append(Component.text("  ⚔ ", NamedTextColor.GOLD))
                .append(Component.text("Duel Request", NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("  From: ", NamedTextColor.GRAY))
                .append(Component.text(sender.getName(), NamedTextColor.AQUA))
                .append(Component.newline())
                .append(Component.text("  Kit: ", NamedTextColor.GRAY))
                .append(Component.text(kitName, NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("  Map: ", NamedTextColor.GRAY))
                .append(Component.text(displayMap, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(acceptWrapped)
                .append(Component.text("   "))
                .append(declineWrapped)
                .append(Component.newline())
                .append(Component.newline())
                .append(SEPARATOR)
                .append(Component.newline());

        target.sendMessage(targetMsg);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    /**
     * Intercept /duelaccept and /dueldecline to prevent them leaking to console/SP.
     */
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage().toLowerCase();
        if (msg.startsWith("/duelaccept ") || msg.startsWith("/dueldecline ")) {
            e.setCancelled(true);
            String[] parts = e.getMessage().split(" ", 2);
            if (parts.length < 2)
                return;

            Player player = e.getPlayer();
            String senderName = parts[1].trim();

            if (msg.startsWith("/duelaccept ")) {
                handleAccept(player, senderName);
            } else {
                handleDecline(player, senderName);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Fallback executor — should not normally be called since we intercept in
        // preprocess
        return true;
    }

    private void handleAccept(Player target, String senderName) {
        Collection<DuelRequest> requests = FightRequest.getDuelRequestsForPlayer(target);
        if (requests == null || requests.isEmpty()) {
            target.sendMessage(Component.text("  ✗ No pending duel request from that player.", NamedTextColor.RED));
            target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
            return;
        }

        StrikePracticeAPI api = StrikePractice.getAPI();

        Set<String> declined = declinedRequests.get(target.getUniqueId());

        for (DuelRequest req : requests) {
            if (req.getDueler().equalsIgnoreCase(senderName)) {
                if (req.hasExpired()
                        || (declined != null && declined.contains(senderName.toLowerCase()))) {
                    target.sendMessage(Component.text("  ✗ That duel request has expired.", NamedTextColor.RED));
                    target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
                    return;
                }

                // Guard: check if target is busy
                if (api.isInFight(target) || api.isInQueue(target)) {
                    target.sendMessage(Component.text("  ✗ You're already in a fight or queue.", NamedTextColor.RED));
                    target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
                    return;
                }

                // Guard: the sender may have gone offline or entered a fight
                // since sending the request
                Player requestSender = Bukkit.getPlayer(senderName);
                if (requestSender == null || !requestSender.isOnline() || api.isInFight(requestSender)) {
                    target.sendMessage(Component.text("  ✗ That player can no longer duel.", NamedTextColor.RED));
                    target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
                    return;
                }

                // Start the duel
                try {
                    Duel duel = (Duel) req.getFight();
                    duel.start();
                } catch (Exception e) {
                    target.sendMessage(Component.text("  ✗ Failed to start duel.", NamedTextColor.RED));
                    target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
                    return;
                }

                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                Player sender = Bukkit.getPlayer(senderName);
                if (sender != null && sender.isOnline()) {
                    sender.playSound(sender.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                    sender.sendMessage(Component.empty()
                            .append(Component.text("  ✓ ", NamedTextColor.GREEN))
                            .append(Component.text(target.getName(), NamedTextColor.AQUA))
                            .append(Component.text(" accepted your duel!", NamedTextColor.GREEN)));
                }
                return;
            }
        }

        target.sendMessage(Component.text("  ✗ No pending duel request from that player.", NamedTextColor.RED));
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
    }

    private void handleDecline(Player target, String senderName) {
        Collection<DuelRequest> requests = FightRequest.getDuelRequestsForPlayer(target);
        if (requests == null || requests.isEmpty()) {
            target.sendMessage(Component.text("  ✗ No pending duel request from that player.", NamedTextColor.RED));
            target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
            return;
        }

        for (DuelRequest req : requests) {
            if (req.getDueler().equalsIgnoreCase(senderName)) {
                // Mark declined so a later /duelaccept can't start it anyway
                declinedRequests.computeIfAbsent(target.getUniqueId(), k -> new HashSet<>())
                        .add(senderName.toLowerCase());
                target.sendMessage(Component.empty()
                        .append(Component.text("  ✗ ", NamedTextColor.RED))
                        .append(Component.text("Duel declined.", NamedTextColor.GRAY)));
                target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 0.8f);

                Player sender = Bukkit.getPlayer(senderName);
                if (sender != null && sender.isOnline()) {
                    sender.sendMessage(Component.empty()
                            .append(Component.text("  ✗ ", NamedTextColor.RED))
                            .append(Component.text(target.getName(), NamedTextColor.AQUA))
                            .append(Component.text(" declined your duel.", NamedTextColor.RED)));
                    sender.playSound(sender.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 0.8f);
                }
                return;
            }
        }

        target.sendMessage(Component.text("  ✗ No pending duel request from that player.", NamedTextColor.RED));
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.0f);
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent e) {
        declinedRequests.remove(e.getPlayer().getUniqueId());
    }
}
