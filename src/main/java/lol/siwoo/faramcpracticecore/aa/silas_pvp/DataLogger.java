package lol.siwoo.faramcpracticecore.aa.silas_pvp;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import ga.strikepractice.events.DuelEndEvent;
import ga.strikepractice.events.DuelStartEvent;
import ga.strikepractice.fights.duel.Duel;
import lol.siwoo.faramcpracticecore.FaraMCPracticeCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DataLogger implements Listener {

    private final FaraMCPracticeCore plugin;
    private final StrikePracticeAPI api;
    private final Map<String, MatchSession> activeSessions;
    private final Map<UUID, PlayerTracker> playerTrackers;
    private final SimpleDateFormat dateFormat;

    public DataLogger(FaraMCPracticeCore plugin) {
        this.plugin = plugin;
        this.api = StrikePractice.getAPI();
        this.activeSessions = new ConcurrentHashMap<>();
        this.playerTrackers = new ConcurrentHashMap<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Start periodic data collection task
        startPeriodicDataCollection();
    }

    private void startPeriodicDataCollection() {
        new BukkitRunnable() {
            @Override
            public void run() {
                collectPeriodicData();
            }
        }.runTaskTimer(plugin, 0L, 5L); // Collect data every 0.5 seconds
    }

    private void collectPeriodicData() {
        for (MatchSession session : activeSessions.values()) {
            if (session.isActive()) {
                session.captureGameState();
            }
        }
    }

    @EventHandler
    public void onFightStart(DuelStartEvent e) {
        String matchId = generateMatchId();
        MatchSession session = new MatchSession(matchId, e.getFight(), this); // Pass 'this' reference
        activeSessions.put(matchId, session);

        // Initialize player trackers
        for (Player player : e.getFight().getPlayersInFight()) {
            playerTrackers.put(player.getUniqueId(), new PlayerTracker(player, matchId));
        }

        plugin.getLogger().info("[silas-pvp-1] Started data logging for match: " + matchId);
    }

    @EventHandler
    public void onFightEnd(DuelEndEvent event) {
        String matchId = findMatchId(event.getFight().getPlayersInFight());
        if (matchId != null) {
            MatchSession session = activeSessions.get(matchId);
            if (session != null) {
                session.endMatch(event);
                saveMatchData(session);
                activeSessions.remove(matchId);
            }

            // Clean up player trackers
            for (Player player : event.getFight().getPlayersInFight()) {
                playerTrackers.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        PlayerTracker tracker = playerTrackers.get(event.getPlayer().getUniqueId());
        if (tracker != null && tracker.isTracking()) {
            tracker.recordMovement(event.getFrom(), event.getTo());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            PlayerTracker tracker = playerTrackers.get(player.getUniqueId());
            if (tracker != null && tracker.isTracking()) {
                tracker.recordDamage(event);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();

            PlayerTracker attackerTracker = playerTrackers.get(attacker.getUniqueId());
            PlayerTracker victimTracker = playerTrackers.get(victim.getUniqueId());

            if (attackerTracker != null && attackerTracker.isTracking()) {
                attackerTracker.recordAttack(event);
            }

            if (victimTracker != null && victimTracker.isTracking()) {
                victimTracker.recordBeingAttacked(event);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        PlayerTracker tracker = playerTrackers.get(event.getEntity().getUniqueId());
        if (tracker != null && tracker.isTracking()) {
            tracker.recordDeath(event);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        PlayerTracker tracker = playerTrackers.get(event.getPlayer().getUniqueId());
        if (tracker != null && tracker.isTracking()) {
            tracker.recordBlockPlace(event);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        PlayerTracker tracker = playerTrackers.get(event.getPlayer().getUniqueId());
        if (tracker != null && tracker.isTracking()) {
            tracker.recordBlockBreak(event);
        }
    }

    private String generateMatchId() {
        return "match_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String findMatchId(List<Player> players) {
        for (MatchSession session : activeSessions.values()) {
            if (session.containsPlayers(players)) {
                return session.getMatchId();
            }
        }
        return null;
    }

    private void saveMatchData(MatchSession session) {
        try {
            File dataDir = new File(plugin.getDataFolder(), "ai_training_data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            // Use custom serialization instead of Gson to avoid reflection issues
            File matchFile = new File(dataDir, session.getMatchId() + ".json");
            
            try (FileWriter writer = new FileWriter(matchFile)) {
                writeCustomJson(writer, session);
            }

            plugin.getLogger().info("[silas-pvp-1] Saved match data: " + matchFile.getName());

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[silas-pvp-1] Failed to save match data: " + e.getMessage(), e);
            
            // Fallback: save as plain text
            saveAsPlainText(session);
        }
    }

    private void writeCustomJson(FileWriter writer, MatchSession session) throws IOException {
        writer.write("{\n");
        
        // Basic match info
        writer.write("  \"matchId\": \"" + session.getMatchId() + "\",\n");
        writer.write("  \"gameMode\": \"" + session.gameMode + "\",\n");
        writer.write("  \"startTime\": " + session.startTime + ",\n");
        writer.write("  \"endTime\": " + session.endTime + ",\n");
        writer.write("  \"winner\": \"" + (session.winner != null ? session.winner : "null") + "\",\n");
        writer.write("  \"startTimeFormatted\": \"" + dateFormat.format(new Date(session.startTime)) + "\",\n");
        writer.write("  \"endTimeFormatted\": \"" + dateFormat.format(new Date(session.endTime)) + "\",\n");
        
        // Players
        writer.write("  \"players\": [\n");
        for (int i = 0; i < session.players.size(); i++) {
            PlayerData player = session.players.get(i);
            writer.write("    {\n");
            writer.write("      \"name\": \"" + player.name + "\",\n");
            writer.write("      \"uuid\": \"" + player.uuid.toString() + "\",\n");
            writer.write("      \"joinTime\": " + player.joinTime + ",\n");
            writer.write("      \"teammates\": [");
            for (int j = 0; j < player.teammates.size(); j++) {
                writer.write("\"" + player.teammates.get(j) + "\"");
                if (j < player.teammates.size() - 1) writer.write(", ");
            }
            writer.write("]\n");
            writer.write("    }");
            if (i < session.players.size() - 1) writer.write(",");
            writer.write("\n");
        }
        writer.write("  ],\n");
        
        // Game states
        writer.write("  \"gameStates\": [\n");
        for (int i = 0; i < session.gameStates.size(); i++) {
            GameState state = session.gameStates.get(i);
            writer.write("    {\n");
            writer.write("      \"timestamp\": " + state.timestamp + ",\n");
            writer.write("      \"playerStates\": [\n");
            
            for (int j = 0; j < state.playerStates.size(); j++) {
                PlayerState playerState = state.playerStates.get(j);
                writer.write("        {\n");
                writer.write("          \"playerId\": \"" + playerState.playerId.toString() + "\",\n");
                writer.write("          \"health\": " + playerState.health + ",\n");
                writer.write("          \"hunger\": " + playerState.hunger + ",\n");
                writer.write("          \"onGround\": " + playerState.onGround + ",\n");
                writer.write("          \"sneaking\": " + playerState.sneaking + ",\n");
                writer.write("          \"sprinting\": " + playerState.sprinting + ",\n");
                writer.write("          \"blocking\": " + playerState.blocking + ",\n");
                writer.write("          \"isHitting\": " + playerState.isHitting + ",\n");
                writer.write("          \"isBeingHit\": " + playerState.isBeingHit + ",\n");
                
                // Location
                writer.write("          \"location\": {\n");
                writer.write("            \"x\": " + playerState.location.x + ",\n");
                writer.write("            \"y\": " + playerState.location.y + ",\n");
                writer.write("            \"z\": " + playerState.location.z + ",\n");
                writer.write("            \"yaw\": " + playerState.location.yaw + ",\n");
                writer.write("            \"pitch\": " + playerState.location.pitch + ",\n");
                writer.write("            \"world\": \"" + playerState.location.world + "\"\n");
                writer.write("          },\n");
                
                // Velocity
                writer.write("          \"velocity\": {\n");
                writer.write("            \"x\": " + playerState.velocity.x + ",\n");
                writer.write("            \"y\": " + playerState.velocity.y + ",\n");
                writer.write("            \"z\": " + playerState.velocity.z + "\n");
                writer.write("          }\n");
                
                writer.write("        }");
                if (j < state.playerStates.size() - 1) writer.write(",");
                writer.write("\n");
            }
            
            writer.write("      ]\n");
            writer.write("    }");
            if (i < session.gameStates.size() - 1) writer.write(",");
            writer.write("\n");
        }
        writer.write("  ],\n");
        
        // Metadata
        writer.write("  \"metadata\": {\n");
        writer.write("    \"totalGameStates\": " + session.gameStates.size() + ",\n");
        writer.write("    \"playerCount\": " + session.players.size() + ",\n");
        writer.write("    \"durationMs\": " + (session.endTime - session.startTime) + "\n");
        writer.write("  }\n");
        
        writer.write("}");
    }

    private void saveAsPlainText(MatchSession session) {
        try {
            File dataDir = new File(plugin.getDataFolder(), "ai_training_data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            File matchFile = new File(dataDir, session.getMatchId() + "_fallback.txt");
            try (FileWriter writer = new FileWriter(matchFile)) {
                writer.write("=== MATCH DATA (FALLBACK FORMAT) ===\n");
                writer.write("Match ID: " + session.getMatchId() + "\n");
                writer.write("Game Mode: " + session.gameMode + "\n");
                writer.write("Start Time: " + dateFormat.format(new Date(session.startTime)) + "\n");
                writer.write("End Time: " + dateFormat.format(new Date(session.endTime)) + "\n");
                writer.write("Duration: " + (session.endTime - session.startTime) + "ms\n");
                writer.write("Winner: " + (session.winner != null ? session.winner : "Unknown") + "\n");
                writer.write("Players: " + session.players.size() + "\n");
                writer.write("Game States Captured: " + session.gameStates.size() + "\n\n");
                
                writer.write("=== PLAYERS ===\n");
                for (PlayerData player : session.players) {
                    writer.write("- " + player.name + " (" + player.uuid + ")\n");
                    writer.write("  Teammates: " + String.join(", ", player.teammates) + "\n");
                }
                
                writer.write("\n=== GAME STATES SUMMARY ===\n");
                writer.write("Total states captured: " + session.gameStates.size() + "\n");
                writer.write("First state: " + (session.gameStates.isEmpty() ? "None" : session.gameStates.get(0).timestamp + "ms") + "\n");
                writer.write("Last state: " + (session.gameStates.isEmpty() ? "None" : session.gameStates.get(session.gameStates.size()-1).timestamp + "ms") + "\n");
            }
            
            plugin.getLogger().info("[silas-pvp-1] Saved fallback match data: " + matchFile.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("[silas-pvp-1] Failed to save fallback match data: " + e.getMessage());
        }
    }

    // Inner classes with simplified structure
    public static class MatchSession {
        private final String matchId;
        private final String gameMode;
        private final List<PlayerData> players;
        private final List<GameState> gameStates;
        private final long startTime;
        private long endTime;
        private boolean active;
        private String winner;
        private final DataLogger dataLogger; // Add this field

        public MatchSession(String matchId, Duel fight, DataLogger dataLogger) {
            this.matchId = matchId;
            this.gameMode = fight.getKit().getName();
            this.players = new ArrayList<>();
            this.gameStates = new ArrayList<>();
            this.startTime = System.currentTimeMillis();
            this.active = true;
            this.dataLogger = dataLogger; // Store the DataLogger reference

            // Initialize players after a short delay to ensure everything is loaded
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player player : fight.getPlayersInFight()) {
                        players.add(new PlayerData(player, fight.getTeammates(player)));
                    }
                }
            }.runTaskLater((FaraMCPracticeCore) Bukkit.getPluginManager().getPlugin("FaraMCPracticeCore"), 5L);
        }

        public void captureGameState() {
            GameState state = new GameState(System.currentTimeMillis() - startTime);

            for (PlayerData playerData : players) {
                Player player = playerData.getPlayer();
                if (player != null && player.isOnline()) {
                    // Use the stored DataLogger reference
                    PlayerTracker tracker = dataLogger.playerTrackers.get(player.getUniqueId());
                    
                    boolean isHitting = false;
                    boolean isBeingHit = false;
                    
                    if (tracker != null) {
                        isHitting = tracker.isCurrentlyHitting();
                        isBeingHit = tracker.isCurrentlyBeingHit();
                    }

                    PlayerState playerState = new PlayerState(
                            player.getUniqueId(),
                            player.getLocation(),
                            player.getHealth(),
                            player.getFoodLevel(),
                            player.getVelocity(),
                            player.isOnGround(),
                            player.isSneaking(),
                            player.isSprinting(),
                            player.isBlocking(),
                            isHitting,
                            isBeingHit
                    );
                    state.addPlayerState(playerState);
                }
            }

            gameStates.add(state);
        }

        public void endMatch(DuelEndEvent e) {
            this.active = false;
            this.endTime = System.currentTimeMillis();

            // Determine winner
            if (e.getWinner() != null) {
                this.winner = e.getWinner().getName();
            }
        }

        public boolean containsPlayers(List<Player> checkPlayers) {
            Set<UUID> sessionPlayerIds = new HashSet<>();
            for (PlayerData pd : players) {
                sessionPlayerIds.add(pd.uuid);
            }

            for (Player player : checkPlayers) {
                if (sessionPlayerIds.contains(player.getUniqueId())) {
                    return true;
                }
            }
            return false;
        }

        // Getters
        public String getMatchId() { return matchId; }
        public boolean isActive() { return active; }
    }

    public static class PlayerTracker {
        private final UUID playerId;
        private final String playerName;
        private final String matchId;
        private final List<PlayerAction> actions;
        private final List<LocationPoint> movementPath;
        private final PlayerStats stats;
        private boolean tracking;
        
        // Combat state tracking
        private long lastHitTime = 0;
        private long lastBeingHitTime = 0;
        private static final long HIT_STATE_DURATION = 1000; // 1 second

        public PlayerTracker(Player player, String matchId) {
            this.playerId = player.getUniqueId();
            this.playerName = player.getName();
            this.matchId = matchId;
            this.actions = new ArrayList<>();
            this.movementPath = new ArrayList<>();
            this.stats = new PlayerStats();
            this.tracking = true;
        }

        public void recordMovement(Location from, Location to) {
            if (!tracking || from.distanceSquared(to) <= 0.01) return;

            movementPath.add(new LocationPoint(to, System.currentTimeMillis()));
            
            double distance = from.distance(to);
            stats.totalDistanceMoved += distance;

            if (distance > 0.1) {
                double speed = distance * 20; // Convert to blocks per second
                stats.averageSpeed = (stats.averageSpeed * stats.movementCount + speed) / (stats.movementCount + 1);
                stats.movementCount++;
            }
        }

        public void recordAttack(EntityDamageByEntityEvent event) {
            if (!tracking) return;

            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();
            
            // Update hit state
            this.lastHitTime = System.currentTimeMillis();

            PlayerAction action = new PlayerAction(
                    "ATTACK",
                    System.currentTimeMillis(),
                    attacker.getLocation()
            );

            actions.add(action);
            stats.attacksMade++;
            stats.totalDamageDealt += event.getFinalDamage();
        }

        public void recordBeingAttacked(EntityDamageByEntityEvent event) {
            if (!tracking) return;
            
            // Update being hit state
            this.lastBeingHitTime = System.currentTimeMillis();
            
            stats.damageReceived += event.getFinalDamage();
        }

        public void recordDamage(EntityDamageEvent event) {
            if (!tracking) return;

            PlayerAction action = new PlayerAction(
                    "DAMAGE_RECEIVED",
                    System.currentTimeMillis(),
                    ((Player) event.getEntity()).getLocation()
            );

            actions.add(action);
        }

        public void recordDeath(PlayerDeathEvent event) {
            if (!tracking) return;

            PlayerAction action = new PlayerAction(
                    "DEATH",
                    System.currentTimeMillis(),
                    event.getEntity().getLocation()
            );

            actions.add(action);
            stats.deaths++;
        }

        public void recordBlockPlace(BlockPlaceEvent event) {
            if (!tracking) return;

            PlayerAction action = new PlayerAction(
                    "BLOCK_PLACE",
                    System.currentTimeMillis(),
                    event.getBlock().getLocation()
            );

            actions.add(action);
            stats.blocksPlaced++;
        }

        public void recordBlockBreak(BlockBreakEvent event) {
            if (!tracking) return;

            PlayerAction action = new PlayerAction(
                    "BLOCK_BREAK",
                    System.currentTimeMillis(),
                    event.getBlock().getLocation()
            );

            actions.add(action);
            stats.blocksBroken++;
        }

        // New methods for combat state tracking
        public boolean isCurrentlyHitting() {
            long currentTime = System.currentTimeMillis();
            return (currentTime - lastHitTime) <= HIT_STATE_DURATION;
        }
        
        public boolean isCurrentlyBeingHit() {
            long currentTime = System.currentTimeMillis();
            return (currentTime - lastBeingHitTime) <= HIT_STATE_DURATION;
        }

        public boolean isTracking() { return tracking; }
        public void stopTracking() { this.tracking = false; }
    }

    // Simplified data structure classes
    public static class PlayerData {
        public final String name;
        public final UUID uuid;
        public final List<String> teammates;
        public final long joinTime;

        public PlayerData(Player player, List<String> teammates) {
            this.name = player.getName();
            this.uuid = player.getUniqueId();
            this.teammates = teammates != null ? new ArrayList<>(teammates) : new ArrayList<>();
            this.joinTime = System.currentTimeMillis();
        }

        public Player getPlayer() {
            return Bukkit.getPlayer(uuid);
        }
    }

    public static class GameState {
        public final long timestamp;
        public final List<PlayerState> playerStates;

        public GameState(long timestamp) {
            this.timestamp = timestamp;
            this.playerStates = new ArrayList<>();
        }

        public void addPlayerState(PlayerState state) {
            playerStates.add(state);
        }
    }

    public static class PlayerState {
        public final UUID playerId;
        public final LocationData location;
        public final double health;
        public final int hunger;
        public final VelocityData velocity;
        public final boolean onGround;
        public final boolean sneaking;
        public final boolean sprinting;
        public final boolean blocking;
        public final boolean isHitting;
        public final boolean isBeingHit;

        public PlayerState(UUID playerId, Location location, double health, int hunger,
                           org.bukkit.util.Vector velocity, boolean onGround, boolean sneaking, 
                           boolean sprinting, boolean blocking, boolean isHitting, boolean isBeingHit) {
            this.playerId = playerId;
            this.location = new LocationData(location);
            this.health = health;
            this.hunger = hunger;
            this.velocity = new VelocityData(velocity);
            this.onGround = onGround;
            this.sneaking = sneaking;
            this.sprinting = sprinting;
            this.blocking = blocking;
            this.isHitting = isHitting;
            this.isBeingHit = isBeingHit;
        }
    }

    public static class PlayerAction {
        public final String type;
        public final long timestamp;
        public final LocationData location;

        public PlayerAction(String type, long timestamp, Location location) {
            this.type = type;
            this.timestamp = timestamp;
            this.location = new LocationData(location);
        }
    }

    public static class PlayerStats {
        public int attacksMade = 0;
        public int deaths = 0;
        public int blocksPlaced = 0;
        public int blocksBroken = 0;
        public double totalDamageDealt = 0.0;
        public double damageReceived = 0.0;
        public double totalDistanceMoved = 0.0;
        public double averageSpeed = 0.0;
        public int movementCount = 0;
    }

    public static class LocationData {
        public final double x, y, z;
        public final float yaw, pitch;
        public final String world;

        public LocationData(Location location) {
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
            this.yaw = location.getYaw();
            this.pitch = location.getPitch();
            this.world = location.getWorld().getName();
        }
    }

    public static class VelocityData {
        public final double x, y, z;

        public VelocityData(org.bukkit.util.Vector velocity) {
            this.x = velocity.getX();
            this.y = velocity.getY();
            this.z = velocity.getZ();
        }
    }

    public static class LocationPoint {
        public final LocationData location;
        public final long timestamp;

        public LocationPoint(Location location, long timestamp) {
            this.location = new LocationData(location);
            this.timestamp = timestamp;
        }
    }
}