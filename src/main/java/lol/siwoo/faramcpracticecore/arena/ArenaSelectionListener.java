package lol.siwoo.faramcpracticecore.arena;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.arena.Arena;
import ga.strikepractice.events.*;
import ga.strikepractice.fights.Fight;
import lol.siwoo.faramcpracticecore.FaraMCPracticeCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.npc.NPC;

import java.util.*;

public class ArenaSelectionListener implements Listener {
    private final FaraMCPracticeCore plugin;
    private final ArenaManager manager;

    private final Set<UUID> pendingPaste = new HashSet<>();
    private final Set<UUID> delayedPlayers = new HashSet<>();
    private final Set<Fight> handledStarts = new HashSet<>();
    private final Set<Fight> handledEnds = new HashSet<>();
    private final Map<Fight, NPC> pendingBots = new WeakHashMap<>();
    // Track the correct paste world for each player while paste is in progress
    private final Map<UUID, String> playerCorrectWorld = new HashMap<>();

    public ArenaSelectionListener(FaraMCPracticeCore plugin, ArenaManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onKitSelect(KitSelectEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("faramcpracticecore.selectarena") && StrikePractice.getAPI().isInQueue(player)) {
            ArenaSelectorGUI.open(player, manager, event.getKit().getName(), () -> {
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // For players waiting on paste: block teleports going to wrong world
        if (pendingPaste.contains(uuid)) {
            String correctWorld = playerCorrectWorld.get(uuid);
            if (correctWorld == null) {
                // Paste still in progress, world not known yet — block ALL teleports
                event.setCancelled(true);
                return;
            }
            if (event.getTo() != null
                    && !event.getTo().getWorld().getName().equals(correctWorld)) {
                // SP is trying to teleport to the original arena world — block it
                event.setCancelled(true);
                return;
            }
            // Teleport is within the correct paste world — allow it (StickToSpawn, etc.)
        }

        // For delayed players (fight end): block all teleports
        if (delayedPlayers.contains(uuid)) {
            event.setCancelled(true);
        }
    }

    // ─── Fight Start Handlers ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBotDuelStart(BotDuelStartEvent event) {
        // Store the bot to teleport it later
        if (event.getBot() != null) {
            pendingBots.put(event.getFight(), event.getBot());
        }
        // Bot fight: use the single human player (null-safe — List.of throws on
        // null, and an exception here would leak the whole session)
        handleFightStart(event.getFight(), singletonPlayerList(event.getPlayer()));
    }

    private static List<Player> singletonPlayerList(Player player) {
        return player != null ? List.of(player) : List.of();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFightStart(FightStartEvent event) {
        // Skip child events — they have their own handlers
        if (event instanceof BotDuelStartEvent)
            return;
        handleFightStart(event.getFight(), event.getFight().getPlayersInFight());
    }

    private void handleFightStart(Fight fight, List<Player> players) {
        if (handledStarts.contains(fight))
            return;
        handledStarts.add(fight);

        Arena spArena = fight.getArena();
        if (spArena == null) {
            // Arena not assigned yet — remove from dedup so next event can retry
            handledStarts.remove(fight);
            return;
        }

        String arenaName = spArena.getName().toLowerCase();
        if (!arenaName.contains("dynamic"))
            return;

        plugin.getLogger().info("[Arena] Processing fight on '" + arenaName + "' | type="
                + fight.getClass().getSimpleName() + " | players=" + players.size());

        // Clear delayedPlayers for anyone starting a new fight —
        // prevents previous fight's end-timer from interfering
        for (Player p : players) {
            if (p != null) {
                delayedPlayers.remove(p.getUniqueId());
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> spArena.setUsing(false), 2L);

        boolean isBuild = fight.getKit() != null && fight.getKit().isBuild();
        manager.getOrAllocateDynamicArena(isBuild);

        ArenaConfig selected = null;
        if (!players.isEmpty()) {
            selected = ArenaSelectorGUI.queuedSelections.remove(players.get(0).getUniqueId());
        }
        if (selected == null && fight.getKit() != null) {
            selected = manager.getRandomArenaForKit(fight.getKit().getName());
        }

        if (selected != null) {
            plugin.getLogger().info("[Arena] Using config '" + selected.getName() + "'");
            startMatch(fight, selected, spArena, players);
        } else {
            plugin.getLogger().warning("[Arena] No arena config found! Kit="
                    + (fight.getKit() != null ? fight.getKit().getName() : "null"));
        }
    }

    private void startMatch(Fight fight, ArenaConfig config, Arena spArena, List<Player> players) {
        for (Player p : players) {
            if (p != null)
                pendingPaste.add(p.getUniqueId());
        }

        manager.createSession(fight, config).thenAccept(session -> {
            if (session == null) {
                // Paste failed — clean up blocking state so players aren't stuck
                for (Player p : players) {
                    if (p != null) {
                        pendingPaste.remove(p.getUniqueId());
                        playerCorrectWorld.remove(p.getUniqueId());
                    }
                }
                // Drop dedup/bot state too so the fight objects don't leak
                handledStarts.remove(fight);
                pendingBots.remove(fight);
                plugin.getLogger().warning("[Arena] Paste failed for '" + config.getName()
                        + "' — fight continues on the SP arena's own location.");
                return;
            }

            // The fight may have ended while the paste was running (e.g. a bot
            // dying instantly at the base arena location). Teleporting players
            // into a session that's being torn down would strand them in the
            // paste world after the fight.
            if (handledEnds.contains(fight) || manager.getSession(fight) != session) {
                for (Player p : players) {
                    if (p != null) {
                        pendingPaste.remove(p.getUniqueId());
                        playerCorrectWorld.remove(p.getUniqueId());
                    }
                }
                pendingBots.remove(fight);
                plugin.getLogger().info("[Arena] Fight ended before paste finished — skipping teleport.");
                return;
            }
            session.setSpArena(spArena);

            Location origin = session.getCenter().clone().add(config.getCenter());

            Location s1 = origin.clone().add(config.getPos1());
            Location s2 = origin.clone().add(config.getPos2());

            Vector dir1 = s2.toVector().subtract(s1.toVector()).setY(0);
            s1.setDirection(dir1);
            Vector dir2 = s1.toVector().subtract(s2.toVector()).setY(0);
            s2.setDirection(dir2);

            // Store the correct paste world for each player (for teleport blocking)
            String pasteWorldName = s1.getWorld().getName();
            for (Player p : players) {
                if (p != null) {
                    playerCorrectWorld.put(p.getUniqueId(), pasteWorldName);
                }
            }

            // Update SP arena locations
            fight.getArena().setLoc1(s1);
            fight.getArena().setLoc2(s2);

            // Explicitly teleport the bot if present. Citizens NPCs can spawn a
            // few ticks after the fight starts, so retry instead of giving up —
            // otherwise the bot fights at the old arena while the player is in
            // the clone.
            NPC bot = pendingBots.remove(fight);
            if (bot != null) {
                teleportBotWhenSpawned(fight, bot, s2, 0);
            }

            // Unblock and teleport players
            for (Player p : players) {
                if (p != null) {
                    pendingPaste.remove(p.getUniqueId());
                    playerCorrectWorld.remove(p.getUniqueId());
                }
            }

            if (players.size() >= 1) {
                boolean success = players.get(0).teleport(s1);
                plugin.getLogger().info("[Arena] Teleported " + players.get(0).getName() + " to "
                        + s1.toVector() + " | Success: " + success);
            }
            if (players.size() >= 2) {
                players.get(1).teleport(s2);
            }

            plugin.getLogger().info("[Arena] Teleported " + players.size() + " player(s) to '"
                    + config.getName() + "' in " + s1.getWorld().getName());
        });
    }

    /**
     * Teleports the bot to its arena spawn once its NPC has actually spawned,
     * retrying every 2 ticks for up to ~2 seconds. Stops early if the fight
     * ends in the meantime (the NPC gets despawned by StrikePractice).
     */
    private void teleportBotWhenSpawned(Fight fight, NPC bot, Location dest, int attempt) {
        if (handledEnds.contains(fight) || manager.getSession(fight) == null) {
            return;
        }
        if (bot.isSpawned()) {
            bot.teleport(dest, PlayerTeleportEvent.TeleportCause.PLUGIN);
            plugin.getLogger().info("[Arena] Force teleported bot " + bot.getName() + " to " + dest.toVector()
                    + (attempt > 0 ? " (after " + attempt + " retries)" : ""));
            return;
        }
        if (attempt >= 20) {
            plugin.getLogger().warning("[Arena] Bot " + bot.getName()
                    + " never spawned — could not move it to the cloned arena.");
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> teleportBotWhenSpawned(fight, bot, dest, attempt + 1), 2L);
    }

    // ─── Dynamic Arena Management ────────────────────────────────────────

    private void cleanupDynamicArena(Fight fight) {
        FightSession session = manager.getSession(fight);
        if (session == null)
            return;

        Arena spArena = session.getSpArena();
        if (spArena == null)
            return;

        String arenaName = spArena.getName().toLowerCase();
        if (!arenaName.contains("dynamic"))
            return;

        boolean isBuild = arenaName.startsWith("dynamicbuild");
        String prefix = isBuild ? "dynamicbuild" : "dynamic";

        long count = StrikePractice.getAPI().getArenas().stream()
                .filter(a -> a.getName().toLowerCase().startsWith(prefix))
                .count();

        if (count > 1 && arenaName.contains("_")) {
            spArena.removeFromStrikePractice();
            plugin.getLogger().info("[Arena] Removed extra SP arena: " + spArena.getName());
        }
    }

    // ─── Fight End Handlers ──────────────────────────────────────────────

    private void handleFightEnd(Fight fight, List<Player> players) {
        if (handledEnds.contains(fight))
            return;
        handledEnds.add(fight);

        if (manager.getSession(fight) == null) {
            // No cloned arena for this fight — nothing to clean up, but drop the
            // dedup entries so the Fight objects don't accumulate forever
            handledEnds.remove(fight);
            handledStarts.remove(fight);
            return;
        }

        Location spawn = StrikePractice.getAPI().getSpawnLocation();

        for (Player p : players) {
            if (p != null)
                delayedPlayers.add(p.getUniqueId());
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            cleanupDynamicArena(fight);

            for (Player p : players) {
                if (p != null && p.isOnline()) {
                    delayedPlayers.remove(p.getUniqueId());
                    // Only teleport to spawn if player is NOT in a new fight
                    if (StrikePractice.getAPI().getFight(p) == null) {
                        p.teleport(spawn);
                    }
                }
            }

            manager.endSession(fight);
            handledEnds.remove(fight);
            handledStarts.remove(fight);
        }, 60L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFightEnd(FightEndEvent event) {
        handleFightEnd(event.getFight(), event.getFight().getPlayersInFight());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDuelEnd(DuelEndEvent event) {
        handleFightEnd(event.getFight(), event.getFight().getPlayersInFight());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBotDuelEnd(BotDuelEndEvent event) {
        handleFightEnd(event.getFight(), singletonPlayerList(event.getPlayer()));
    }
}