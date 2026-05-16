package lol.siwoo.faramcpracticecore.gamemode;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import ga.strikepractice.arena.DefaultCachedBlockChange;
import ga.strikepractice.events.FightEndEvent;
import ga.strikepractice.events.FightStartEvent;
import ga.strikepractice.events.PlayerStartSpectatingEvent;
import lol.siwoo.faramcpracticecore.FaraMCPracticeCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FireballFight implements Listener {

    private final FaraMCPracticeCore plugin;
    private final StrikePracticeAPI api;
    private final Map<UUID, Long> cooldownMap;
    private static final long COOLDOWN_DURATION = 5000;
    private final Map<UUID, Boolean> isInFireballfight;
    private final Map<UUID, Boolean> isDead;
    private final Map<UUID, Boolean> isbedBroken;
    private final Map<UUID, Location> startPositions;
    private final Map<String, String> fightIds;
    private final Map<String, List<BedBreakData>> fightBedBreaks;
    private final Map<UUID, RespawnTaskData> activeRespawnTasks;
    private int fightCounter = 0;

    private enum RespawnType {
        VOID_FALL, DEATH
    }

    private static class RespawnTaskData {
        final Player player;
        final RespawnType type;
        final Location oldLocation;
        int ticks;

        RespawnTaskData(Player player, RespawnType type, Location oldLocation) {
            this.player = player;
            this.type = type;
            this.oldLocation = oldLocation;
            this.ticks = 0;
        }
    }

    public FireballFight(FaraMCPracticeCore plugin) {
        this.plugin = plugin;
        this.api = StrikePractice.getAPI();
        this.cooldownMap = new HashMap<>();
        this.isInFireballfight = new HashMap<>();
        this.isDead = new HashMap<>();
        this.isbedBroken = new HashMap<>();
        this.startPositions = new HashMap<>();
        this.fightIds = new HashMap<>();
        this.fightBedBreaks = new HashMap<>();
        this.activeRespawnTasks = new HashMap<>();

        startTickTask();
    }

    private static class BedBreakData {
        final Location headLocation;
        final Location footLocation;
        final BlockData headBlockData;
        final BlockData footBlockData;

        BedBreakData(Location headLoc, Location footLoc, BlockData headData, BlockData footData) {
            this.headLocation = headLoc;
            this.footLocation = footLoc;
            this.headBlockData = headData;
            this.footBlockData = footData;
        }
    }

    private void startTickTask() {
        new BukkitRunnable() {
            private int cleanupTicks = 0;

            @Override
            public void run() {
                // Cooldown cleanup every 20 ticks
                if (++cleanupTicks >= 20) {
                    long currentTime = System.currentTimeMillis();
                    cooldownMap.entrySet().removeIf(entry -> currentTime - entry.getValue() > COOLDOWN_DURATION);
                    cleanupTicks = 0;
                }

                // Respawn tasks
                if (activeRespawnTasks.isEmpty()) {
                    return;
                }

                Iterator<Map.Entry<UUID, RespawnTaskData>> iterator = activeRespawnTasks.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<UUID, RespawnTaskData> entry = iterator.next();
                    RespawnTaskData task = entry.getValue();
                    Player p = task.player;
                    UUID pid = p.getUniqueId();
                    task.ticks++;

                    if (task.type == RespawnType.VOID_FALL) {
                        if (task.ticks == 5) {
                            p.setAllowFlight(true);
                            p.teleport(task.oldLocation);
                            p.setFlying(true);
                        } else if (task.ticks == 80) {
                            isDead.remove(pid);
                        } else if (task.ticks == 85) {
                            p.setAllowFlight(false);
                            p.setFlying(false);
                            iterator.remove();
                        }
                    } else if (task.type == RespawnType.DEATH) {
                        if (task.ticks == 20) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                            p.showTitle(Title.title(
                                    Component.text("You died!", NamedTextColor.RED, TextDecoration.BOLD),
                                    Component.text("You will respawn in 3 seconds", NamedTextColor.WHITE)));
                        } else if (task.ticks == 40) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                            p.showTitle(Title.title(
                                    Component.text("You died!", NamedTextColor.RED, TextDecoration.BOLD),
                                    Component.text("You will respawn in 2 seconds", NamedTextColor.WHITE)));
                        } else if (task.ticks == 60) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                            p.showTitle(Title.title(
                                    Component.text("You died!", NamedTextColor.RED, TextDecoration.BOLD),
                                    Component.text("You will respawn in 1 seconds", NamedTextColor.WHITE)));
                        } else if (task.ticks == 80) {
                            Location spawnLocation = startPositions.get(pid);
                            if (spawnLocation != null) {
                                p.playSound(spawnLocation, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                                p.showTitle(Title.title(
                                        Component.text("Respawned!", NamedTextColor.GREEN, TextDecoration.BOLD),
                                        Component.empty()));
                                p.teleport(spawnLocation);
                            }
                            p.removePotionEffect(PotionEffectType.INVISIBILITY);
                            p.setFlying(false);
                            p.setAllowFlight(false);
                            isDead.remove(pid);
                        } else if (task.ticks == 85) {
                            Location spawnLocation = startPositions.get(pid);
                            if (spawnLocation != null) {
                                p.teleport(spawnLocation);
                            }
                            iterator.remove();
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    @EventHandler
    public void onFightStart(FightStartEvent e) {
        if (!e.getFight().getKit().getName().equalsIgnoreCase("fireballfight")) {
            return;
        }

        String fightId = "fireballfight_" + (++fightCounter) + "_" + System.currentTimeMillis();
        fightBedBreaks.put(fightId, new ArrayList<>());

        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("FireballFight match started. Players: " + e.getFight().getPlayersInFight());

                e.getFight().getPlayersInFight().forEach(p -> {
                    UUID playerId = p.getUniqueId();
                    cooldownMap.put(playerId, System.currentTimeMillis());
                    isInFireballfight.put(playerId, true);
                    fightIds.put(playerId.toString(), fightId);

                    startPositions.put(playerId, p.getLocation().clone());
                    plugin.getLogger().info("Cached starting position for " + p.getName() + ": " + p.getLocation());
                });
            }
        }.runTaskLater(plugin, 40L);
    }

    @EventHandler
    public void onFightEnd(FightEndEvent e) {
        if (!e.getFight().getKit().getName().equalsIgnoreCase("fireballfight")) {
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
            plugin.getLogger().info("Fight ended with ID: " + fightId + ". Starting bed rollback in 1 second...");

            // Schedule rollback after 1 second (20 ticks)
            new BukkitRunnable() {
                @Override
                public void run() {
                    rollbackBeds(finalFightId);
                }
            }.runTaskLater(plugin, 20L);
        }

        e.getFight().getPlayersInFight().forEach(p -> {
            UUID playerId = p.getUniqueId();
            cooldownMap.remove(playerId);
            isInFireballfight.remove(playerId);
            startPositions.remove(playerId);
            fightIds.remove(playerId.toString());
            isbedBroken.remove(playerId);
            isDead.remove(playerId);
            activeRespawnTasks.remove(playerId);
        });
    }

    private void rollbackBeds(String fightId) {
        List<BedBreakData> bedBreaks = fightBedBreaks.get(fightId);
        if (bedBreaks == null || bedBreaks.isEmpty()) {
            plugin.getLogger().info("No beds to rollback for fight ID: " + fightId);
            return;
        }

        plugin.getLogger().info("Rolling back " + bedBreaks.size() + " bed breaks for fight ID: " + fightId);

        for (BedBreakData bedData : bedBreaks) {
            // Restore head block
            bedData.headLocation.getBlock().setBlockData(bedData.headBlockData, false);

            // Restore foot block
            bedData.footLocation.getBlock().setBlockData(bedData.footBlockData, false);

            plugin.getLogger()
                    .info("Restored bed at head: " + bedData.headLocation + ", foot: " + bedData.footLocation);
        }

        // Clean up bed break data after rollback
        fightBedBreaks.remove(fightId);
        plugin.getLogger().info("Cleaned up bed break data for fight ID: " + fightId);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        UUID playerId = e.getPlayer().getUniqueId();
        cooldownMap.remove(playerId);
        isInFireballfight.remove(playerId);
        isDead.remove(playerId);
        isbedBroken.remove(playerId);
        startPositions.remove(playerId);
        fightIds.remove(playerId.toString());
        activeRespawnTasks.remove(playerId);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID playerId = p.getUniqueId();

        if (api.getFight(p) == null) {
            return;
        }
        if (!api.getFight(p).getKit().getName().equalsIgnoreCase("fireballfight")) {
            return;
        }

        if (Boolean.TRUE.equals(isbedBroken.get(playerId))
                && p.getLocation().getY() < api.getFight(p).getArena().getLoc1().getY() - 8
                && !Boolean.TRUE.equals(isDead.get(playerId))) {
            p.damage(69420.0);
            isbedBroken.remove(playerId);
            return;
        }

        if (Boolean.TRUE.equals(isInFireballfight.get(playerId))
                && p.getLocation().getY() < api.getFight(p).getArena().getLoc1().getY() - 8
                && !Boolean.TRUE.equals(isDead.get(playerId))) {

            Location oldlocation = p.getLocation().clone();
            Location location = new Location(p.getLocation().getWorld(), p.getLocation().getX(), -80,
                    p.getLocation().getZ());

            isDead.put(playerId, true);
            p.teleport(location);

            activeRespawnTasks.put(playerId, new RespawnTaskData(p, RespawnType.VOID_FALL, oldlocation));
        }

        if (Boolean.TRUE.equals(isInFireballfight.get(playerId))
                && p.getLocation().getY() <= api.getFight(p).getArena().getLoc1().getY() - 7
                && Boolean.TRUE.equals(isDead.get(playerId))) {
            Location teleportLoc = p.getLocation().clone();
            teleportLoc.add(0, 5, 0);
            p.teleport(teleportLoc);
        }
    }

    @EventHandler
    public void onPlayerDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player))
            return;
        Player p = (Player) e.getEntity();
        UUID playerId = p.getUniqueId();

        if (api.getFight(p) == null) {
            return;
        }
        if (!api.getFight(p).getKit().getName().equalsIgnoreCase("fireballfight")) {
            return;
        }

        if (Boolean.TRUE.equals(isbedBroken.get(playerId))) {
            if (p.getHealth() - e.getFinalDamage() <= 1f) {
                p.damage(69420.0);
                isbedBroken.remove(playerId);
            }
        }
    }

    @EventHandler
    public void onPlayerBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        UUID playerId = p.getUniqueId();

        if (api.getFight(p) == null) {
            return;
        }
        if (!api.getFight(p).getKit().getName().equalsIgnoreCase("fireballfight")) {
            return;
        }

        if (Boolean.TRUE.equals(isInFireballfight.get(playerId))) {
            if (e.getBlock().getY() > api.getFight(p).getArena().getLoc1().getY() + 8
                    || e.getBlock().getY() < api.getFight(p).getArena().getLoc1().getY() - 8
                    || isInCooldown(playerId)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        UUID playerId = p.getUniqueId();

        if (api.getFight(p) == null) {
            return;
        }
        if (!api.getFight(p).getKit().getName().equalsIgnoreCase("fireballfight")
                || !Boolean.TRUE.equals(isInFireballfight.get(playerId))) {
            return;
        }

        if (!(e.getBlock().getBlockData() instanceof Bed)) {
            return;
        }

        String fightId = fightIds.get(playerId.toString());
        if (fightId == null) {
            plugin.getLogger().warning("No fight ID found for player: " + p.getName());
            plugin.getLogger().info("Available fight IDs: " + fightIds.keySet());
            return;
        }

        plugin.getLogger().info("Fight ID: " + fightId);

        int x1 = api.getFight(p).getArena().getLoc1().getBlockX();
        int y1 = api.getFight(p).getArena().getLoc1().getBlockY();
        int z1 = api.getFight(p).getArena().getLoc1().getBlockZ();

        int x2 = api.getFight(p).getArena().getLoc2().getBlockX();
        int y2 = api.getFight(p).getArena().getLoc2().getBlockY();
        int z2 = api.getFight(p).getArena().getLoc2().getBlockZ();

        plugin.getLogger().info("Arena loc1: " + x1 + ", " + y1 + ", " + z1);
        plugin.getLogger().info("Arena loc2: " + x2 + ", " + y2 + ", " + z2);

        int x = e.getBlock().getX();
        int y = e.getBlock().getY();
        int z = e.getBlock().getZ();

        Location startPos = startPositions.get(playerId);
        if (startPos == null) {
            return;
        }

        int sx = startPos.getBlockX();
        int sy = startPos.getBlockY();
        int sz = startPos.getBlockZ();

        int playerTeam = 0;

        String playerTeamResult = compareCoords(sx, sy, sz, x1, y1, z1, x2, y2, z2);

        if (playerTeamResult.equals("1")) {
            playerTeam = 1; // team 1
        } else if (playerTeamResult.equals("2")) {
            playerTeam = 2; // team 2
        }

        String bedTeamResult = compareCoords(x, y, z, x1, y1, z1, x2, y2, z2);

        if (Boolean.TRUE.equals(isInFireballfight.get(playerId)) && !isInCooldown(playerId)) {
            if (bedTeamResult.equals("1")) {
                if (playerTeam == 2) {
                    handleBedBreak(e, fightId, p);
                } else {
                    e.setCancelled(true);
                }
            } else if (bedTeamResult.equals("2")) {
                if (playerTeam == 1) {
                    handleBedBreak(e, fightId, p);
                } else {
                    e.setCancelled(true);
                }
            } else {
                e.setCancelled(true);
            }
        } else {
            e.setCancelled(true);
        }
    }

    private void handleBedBreak(BlockBreakEvent e, String fightId, Player p) {
        if (e.getBlock().getBlockData() instanceof Bed) {
            Bed bedData = (Bed) e.getBlock().getBlockData();
            Block headBlock;
            Block footBlock;

            if (bedData.getPart() == Bed.Part.HEAD) {
                headBlock = e.getBlock();
                footBlock = headBlock.getRelative(bedData.getFacing().getOppositeFace());
            } else {
                footBlock = e.getBlock();
                headBlock = footBlock.getRelative(bedData.getFacing());
            }

            // Log the bed break data before destroying
            BedBreakData breakData = new BedBreakData(
                    headBlock.getLocation().clone(),
                    footBlock.getLocation().clone(),
                    headBlock.getBlockData().clone(),
                    footBlock.getBlockData().clone());

            fightBedBreaks.get(fightId).add(breakData);
            e.setCancelled(false);
            e.setDropItems(false);

            if (footBlock.getBlockData() instanceof Bed) {
                footBlock.setType(Material.AIR);
                api.getFight(p).addBlockChange(new DefaultCachedBlockChange(footBlock.getLocation(), footBlock));
            }
            if (headBlock.getBlockData() instanceof Bed && !headBlock.equals(e.getBlock())) {
                headBlock.setType(Material.AIR);
                api.getFight(p).addBlockChange(new DefaultCachedBlockChange(headBlock.getLocation(), headBlock));
            }

            api.getFight(p).addBlockChange(new DefaultCachedBlockChange(e.getBlock().getLocation(), e.getBlock()));

            api.getFight(p).getPlayersInFight().forEach(player -> {
                java.util.List<String> teammates = api.getFight(player).getTeammates(player);
                boolean sameTeam = teammates.contains(p.getName()) || player.equals(p);

                if (!sameTeam) {
                    isbedBroken.put(player.getUniqueId(), true);
                    player.showTitle(Title.title(
                            Component.text("Bed Destroyed", NamedTextColor.RED, TextDecoration.BOLD),
                            Component.text("You can no longer respawn", NamedTextColor.WHITE)));
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                }
            });
        }
    }

    private boolean isInCooldown(UUID playerId) {
        Long cooldownStart = cooldownMap.get(playerId);
        if (cooldownStart == null) {
            return false;
        }
        return System.currentTimeMillis() - cooldownStart < COOLDOWN_DURATION;
    }

    public String compareCoords(int x, int y, int z, int x1, int y1, int z1, int x2, int y2, int z2) {
        int diffx1 = x1 - x;
        int diffy1 = y1 - y;
        int diffz1 = z1 - z;
        int diff1 = Math.abs(diffx1) + Math.abs(diffy1) + Math.abs(diffz1);

        int diffx2 = x2 - x;
        int diffy2 = y2 - y;
        int diffz2 = z2 - z;
        int diff2 = Math.abs(diffx2) + Math.abs(diffy2) + Math.abs(diffz2);

        if (diff1 < diff2) {
            return "1";
        } else if (diff1 > diff2) {
            return "2";
        } else {
            return "0";
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) {
            return;
        }

        Player p = (Player) e.getEntity();

        if (api.getFight(p) == null) {
            return;
        }
        if (!api.getFight(p).getKit().getName().equalsIgnoreCase("fireballfight")) {
            return;
        }

        if (isInFireballfight.get(p.getUniqueId()) == null) {
            return;
        }

        if (p.getHealth() - e.getFinalDamage() <= 0.0f) {
            if (Boolean.TRUE.equals(isInFireballfight.get(p.getUniqueId()))) {
                isDead.put(p.getUniqueId(), true);
            }
        }
    }

    @EventHandler
    public void onStartSpectate(PlayerStartSpectatingEvent e) {
        Player p = e.getPlayer();
        UUID pid = p.getUniqueId();

        if (!isInFireballfight.containsKey(pid) || !isInFireballfight.get(pid) || !isDead.containsKey(pid)) {
            return;
        }

        if (isInFireballfight.get(pid).equals(Boolean.TRUE)
                && isDead.get(pid).equals(Boolean.TRUE)) {
            e.setCancelled(true);

            p.getInventory().clear();
            p.getInventory().setArmorContents(new ItemStack[4]);
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 1, false, false));
            p.setAllowFlight(true);
            p.setFlying(true);

            api.getFight(p).getPlayersInFight().forEach(player -> {
                player.sendMessage(Component.text(p.getName() + " died", NamedTextColor.GRAY));
            });

            activeRespawnTasks.put(pid, new RespawnTaskData(p, RespawnType.DEATH, null));
        }
    }
}