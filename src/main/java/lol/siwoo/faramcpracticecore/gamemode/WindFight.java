package lol.siwoo.faramcpracticecore.gamemode;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import ga.strikepractice.events.FightEndEvent;
import ga.strikepractice.events.FightStartEvent;
import lol.siwoo.faramcpracticecore.FaraMCPracticeCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WindFight implements Listener {

    private final FaraMCPracticeCore plugin;
    private final StrikePracticeAPI api = StrikePractice.getAPI();
    private final Map<String, String> fightIds;
    private final Map<String, Long> lastLaunch = new HashMap<>();
    private final Map<String, Long> lastPush = new HashMap<>();
    private int fightCounter = 0;

    public WindFight(FaraMCPracticeCore plugin) {
        this.plugin = plugin;
        this.fightIds = new HashMap<>();
    }

    @EventHandler
    public void onFightStart(FightStartEvent e) {
        if (!e.getFight().getKit().getName().equalsIgnoreCase("windfight")) {
            return;
        }

        String fightId = "windfight_" + (++fightCounter) + "_" + System.currentTimeMillis();

        new BukkitRunnable() {
            @Override
            public void run() {
                e.getFight().getPlayersInFight().forEach(p -> {
                    UUID playerId = p.getUniqueId();
                    fightIds.put(playerId.toString(), fightId);
                    p.setGameMode(GameMode.ADVENTURE);
                });
            }
        }.runTaskLater(plugin, 2L);
    }

    @EventHandler
    public void onFightEnd(FightEndEvent e) {
        if (!e.getFight().getKit().getName().equalsIgnoreCase("windfight")) {
            return;
        }

        String fightId = null;
        for (Player p : e.getFight().getPlayersInFight()) {
            fightId = fightIds.get(p.getUniqueId().toString());
            if (fightId != null) {
                break;
            }
        }

        if (fightId != null) {
            final String finalFightId = fightId;
            plugin.getLogger().info("Fight ended with ID: " + fightId + ".");
        }

        e.getFight().getPlayersInFight().forEach(p -> {
            UUID playerId = p.getUniqueId();
            fightIds.remove(playerId.toString());
            lastLaunch.remove(playerId.toString());
            lastPush.remove(playerId.toString());
            // Restore the game mode changed at fight start — otherwise players
            // stay stuck in adventure mode after the fight
            if (p.getGameMode() == GameMode.ADVENTURE) {
                p.setGameMode(GameMode.SURVIVAL);
            }
        });
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        if (!fightIds.containsKey(p.getUniqueId().toString())) {
            return;
        }

        if (e.getItem() == null || !e.getItem().hasItemMeta() || e.getItem().getItemMeta().getDisplayName() == null) {
            return;
        }

        String itemName = e.getItem().getItemMeta().getDisplayName();

        if (itemName.contains("Launch ")) {
            Long lastLaunchTime = lastLaunch.get(p.getUniqueId().toString());
            if (lastLaunchTime != null && (System.currentTimeMillis() - lastLaunchTime) < 2000) {
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 1.0f);
                p.sendActionBar(Component.text("Launch is available after " + ((2000 - (System.currentTimeMillis() - lastLaunchTime) + 999) / 1000) + "s")
                        .color(NamedTextColor.GRAY));
                return;
            }

            lastLaunch.put(p.getUniqueId().toString(), System.currentTimeMillis());
            launchPlayer(p);
        } else if (itemName.contains("Push ")) {
            Long lastPushTime = lastPush.get(p.getUniqueId().toString());
            if (lastPushTime != null && (System.currentTimeMillis() - lastPushTime) < 2000) {
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 1.0f);
                p.sendActionBar(Component.text("Push is available after " + ((2000 - (System.currentTimeMillis() - lastPushTime) + 999) / 1000) + "s")
                        .color(NamedTextColor.GRAY));
                return;
            }

            lastPush.put(p.getUniqueId().toString(), System.currentTimeMillis());
            pushPlayer(p);
        }
    }

    public void launchPlayer(Player p) {
        World world = p.getWorld();
        Location center = p.getLocation();
        Vector direction = p.getLocation().getDirection().normalize();

        p.setVelocity(direction.multiply(3));
        world.spawnParticle(Particle.EXPLOSION, center, 10, 0.2, 0.2, 0.2, 0.05);
    }

    public void pushPlayer(Player p) {
        World world = p.getWorld();
        Location center = p.getLocation();

        world.getNearbyPlayers(center, 10, 10, 10).forEach(nbPlayer -> {
            if (nbPlayer != p && fightIds.containsKey(nbPlayer.getUniqueId().toString())) {
                Location entityLoc = nbPlayer.getLocation();
                Vector push = entityLoc.toVector().subtract(center.toVector());

                if (push.length() > 0) {
                    push.normalize().multiply(1.5);
                    push.setY(1.05);
                    nbPlayer.setVelocity(push);

                    world.spawnParticle(Particle.EXPLOSION, entityLoc, 10, 0.2, 0.2, 0.2, 0.05);
                }
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        UUID playerId = e.getPlayer().getUniqueId();
        boolean wasInWindFight = fightIds.remove(playerId.toString()) != null;
        lastLaunch.remove(playerId.toString());
        lastPush.remove(playerId.toString());
        // Game mode persists in player data — restore it so a mid-fight
        // disconnect doesn't leave the player in adventure mode forever
        if (wasInWindFight && e.getPlayer().getGameMode() == GameMode.ADVENTURE) {
            e.getPlayer().setGameMode(GameMode.SURVIVAL);
        }
    }
}