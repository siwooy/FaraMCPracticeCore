package lol.siwoo.faramcpracticecore.arena;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.block.BlockTypes;
import ga.strikepractice.StrikePractice;
import ga.strikepractice.arena.Arena;
import ga.strikepractice.fights.Fight;
import lol.siwoo.faramcpracticecore.FaraMCPracticeCore;
import org.bukkit.*;
import org.bukkit.util.Vector;
import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;

public class ArenaManager {
    // Chunk radius around a slot center that gets force-loaded while in use
    private static final int CHUNK_TICKET_RADIUS = 2;
    private static final int SLOT_SPACING = 5000;

    private final FaraMCPracticeCore plugin;
    private final Map<String, ArenaConfig> arenas = new HashMap<>();
    private final Map<Fight, FightSession> activeSessions = new ConcurrentHashMap<>();
    private final List<World> pasteWorlds = new ArrayList<>();
    // Slots whose fights have ended and whose blocks are cleared — safe to reuse.
    // Recycling keeps coordinates bounded instead of drifting forever outward.
    private final Deque<Location> freeSlots = new ConcurrentLinkedDeque<>();
    private final Random random = new Random();
    private final File arenaFolder;
    private int nextXOffset = 0, currentWorldIndex = 0;

    public ArenaManager(FaraMCPracticeCore plugin) {
        this.plugin = plugin;
        this.arenaFolder = new File(plugin.getDataFolder(), "arena");
        if (!arenaFolder.exists())
            arenaFolder.mkdirs();
        setupWorlds();
        loadArenas();

        // Initialize dynamic SP arenas after StrikePractice is fully loaded
        Bukkit.getScheduler().runTaskLater(plugin, this::initDynamicSpArenas, 40L);
    }

    private void setupWorlds() {
        String[] names = { "pasteArena1", "pasteArena2", "pasteArena3" };
        for (String name : names) {
            World world = Bukkit.getWorld(name);
            if (world == null) {
                world = new WorldCreator(name).type(WorldType.FLAT)
                        .generatorSettings("{\"layers\": [], \"biome\":\"minecraft:the_void\"}")
                        .generateStructures(false).createWorld();
            }
            if (world != null) {
                world.addPluginChunkTicket(0, 0, plugin);
                pasteWorlds.add(world);
            }
        }
    }

    /**
     * On startup: remove ALL extra dynamic arenas (dynamic_2, dynamicbuild_3, etc.)
     * then ensure the base "dynamic" and "dynamicbuild" arenas exist and are
     * properly configured.
     * All locations are set in minecraft:world (the main world).
     */
    private void initDynamicSpArenas() {
        try {
            World mainWorld = Bukkit.getWorld("world");
            if (mainWorld == null) {
                plugin.getLogger().warning("Could not find 'world' for dynamic arena init!");
                return;
            }

            // Anchor the base arenas at the practice spawn: fighters (especially
            // bots, which aren't teleport-blocked) stand here while the schematic
            // paste runs, so it must be solid ground — not a point in mid-air.
            Location spawnLoc = StrikePractice.getAPI().getSpawnLocation();
            if (spawnLoc == null)
                spawnLoc = new Location(mainWorld, 0, 100, 0);

            // Remove all extra dynamic arenas (anything with underscore like dynamic_2,
            // dynamicbuild_3)
            List<Arena> toRemove = new ArrayList<>();
            boolean hasDynamic = false;
            boolean hasDynamicBuild = false;

            for (Arena a : StrikePractice.getAPI().getArenas()) {
                String name = a.getName().toLowerCase();
                if (name.equals("dynamic")) {
                    hasDynamic = true;
                } else if (name.equals("dynamicbuild")) {
                    hasDynamicBuild = true;
                } else if (name.startsWith("dynamic")) {
                    // Extra dynamic arena (dynamic_2, dynamicbuild_3, etc.) — remove it
                    toRemove.add(a);
                }
            }

            for (Arena a : toRemove) {
                a.removeFromStrikePractice();
                plugin.getLogger().info("Removed extra SP arena: " + a.getName());
            }

            // Ensure "dynamic" exists — non-build arena on world
            if (hasDynamic) {
                Arena existing = StrikePractice.getAPI().getArena("dynamic");
                if (existing != null) {
                    existing.setLoc1(spawnLoc.clone());
                    existing.setLoc2(spawnLoc.clone());
                    existing.setCenter(spawnLoc.clone());
                    existing.setBuild(false);
                    existing.setUsing(false);
                    existing.saveForStrikePractice();
                    plugin.getLogger().info("Reconfigured SP arena: dynamic");
                }
            } else {
                createBaseSpArena("dynamic", spawnLoc, false);
            }

            // Ensure "dynamicbuild" exists — build arena on world
            if (hasDynamicBuild) {
                Arena existing = StrikePractice.getAPI().getArena("dynamicbuild");
                if (existing != null) {
                    existing.setLoc1(spawnLoc.clone());
                    existing.setLoc2(spawnLoc.clone());
                    existing.setCenter(spawnLoc.clone());
                    existing.setBuild(true);
                    existing.setUsing(false);
                    existing.saveForStrikePractice();
                    plugin.getLogger().info("Reconfigured SP arena: dynamicbuild");
                }
            } else {
                createBaseSpArena("dynamicbuild", spawnLoc, true);
            }

            plugin.getLogger().info("Dynamic SP arenas initialized on world '" + mainWorld.getName() + "'.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to init dynamic SP arenas: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new base SP arena by cloning an existing template.
     */
    private void createBaseSpArena(String name, Location loc, boolean isBuild) {
        try {
            List<Arena> existing = StrikePractice.getAPI().getArenas();
            if (existing.isEmpty()) {
                plugin.getLogger().warning("No existing SP arenas to use as template for '" + name + "'");
                return;
            }

            Arena template = existing.get(0);
            Map<String, Object> data = template.serialize();
            data.put("name", name);
            data.put("loc1", loc.clone());
            data.put("loc2", loc.clone());
            data.put("center", loc.clone());

            Arena newArena = (Arena) org.bukkit.configuration.serialization.ConfigurationSerialization
                    .deserializeObject(data, template.getClass());

            if (newArena != null) {
                newArena.setLoc1(loc.clone());
                newArena.setLoc2(loc.clone());
                newArena.setCenter(loc.clone());
                newArena.setBuild(isBuild);
                newArena.setUsing(false);
                newArena.setKits(new ArrayList<>());
                newArena.saveForStrikePractice();
                plugin.getLogger().info("Created SP arena: " + name + " (build=" + isBuild + ")");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create SP arena '" + name + "': " + e.getMessage(), e);
        }
    }

    public void loadArenas() {
        arenas.clear();
        File[] files = arenaFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File f : files)
                arenas.put(f.getName().replace(".yml", "").toLowerCase(), new ArenaConfig(f));
        }
    }

    public CompletableFuture<FightSession> createSession(Fight fight, ArenaConfig config) {
        if (pasteWorlds.isEmpty())
            return CompletableFuture.completedFuture(null);

        // Guard against duplicate start events creating two sessions (and two
        // pastes) for the same fight
        FightSession existing = activeSessions.get(fight);
        if (existing != null)
            return CompletableFuture.completedFuture(existing);

        Location center = allocateSlot();
        addChunkTickets(center);

        FightSession session = new FightSession(fight, config, center);
        activeSessions.put(fight, session);

        CompletableFuture<Boolean> paste = pasteArena(config, center);
        session.setPasteFuture(paste);

        return paste.thenApply(success -> {
            if (!success) {
                // Paste failed — tear the session down so players are never
                // teleported into an empty slot, and recycle the slot.
                activeSessions.remove(fight, session);
                removeChunkTickets(center);
                freeSlots.addLast(center);
                return null;
            }
            return session;
        });
    }

    /**
     * Reuses a slot freed by a finished fight when possible; otherwise carves a
     * new one out of the paste worlds.
     */
    private Location allocateSlot() {
        Location recycled = freeSlots.pollFirst();
        if (recycled != null)
            return recycled;

        World world = pasteWorlds.get(currentWorldIndex);
        Location center = new Location(world, nextXOffset, 100, 0);

        currentWorldIndex = (currentWorldIndex + 1) % pasteWorlds.size();
        if (currentWorldIndex == 0)
            nextXOffset += SLOT_SPACING;
        return center;
    }

    private void addChunkTickets(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX() >> 4;
        int cz = center.getBlockZ() >> 4;
        for (int dx = -CHUNK_TICKET_RADIUS; dx <= CHUNK_TICKET_RADIUS; dx++) {
            for (int dz = -CHUNK_TICKET_RADIUS; dz <= CHUNK_TICKET_RADIUS; dz++) {
                world.getChunkAt(cx + dx, cz + dz).load(true);
                world.addPluginChunkTicket(cx + dx, cz + dz, plugin);
            }
        }
    }

    private void removeChunkTickets(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX() >> 4;
        int cz = center.getBlockZ() >> 4;
        for (int dx = -CHUNK_TICKET_RADIUS; dx <= CHUNK_TICKET_RADIUS; dx++) {
            for (int dz = -CHUNK_TICKET_RADIUS; dz <= CHUNK_TICKET_RADIUS; dz++) {
                world.removePluginChunkTicket(cx + dx, cz + dz, plugin);
            }
        }
    }

    /**
     * Pastes the arena schematic asynchronously. The returned future completes on
     * the main thread with {@code true} only if every block was written; any
     * failure (missing schematic, unknown format, WorldEdit error) completes it
     * with {@code false} so callers can abort the fight setup instead of
     * teleporting players into a void slot.
     */
    private CompletableFuture<Boolean> pasteArena(ArenaConfig config, Location center) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        File file = new File(arenaFolder, config.getSchematicName());
        if (!file.exists()) {
            plugin.getLogger().warning("Schematic not found: " + file.getAbsolutePath());
            future.complete(false);
            return future;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            plugin.getLogger().warning("Unknown schematic format: " + file.getAbsolutePath());
            future.complete(false);
            return future;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                Clipboard cb = reader.read();

                try (EditSession session = WorldEdit.getInstance()
                        .newEditSession(BukkitAdapter.adapt(center.getWorld()))) {
                    session.setSideEffectApplier(SideEffectSet.none());

                    Vector offset = config.getCenter();
                    BlockVector3 pastePos = BlockVector3.at(
                            center.getX() + offset.getX(),
                            center.getY() + offset.getY(),
                            center.getZ() + offset.getZ());

                    Operations.complete(new ClipboardHolder(cb).createPaste(session)
                            .to(pastePos)
                            .ignoreAirBlocks(false).build());

                    // Flush FAWE queue to ensure all blocks are written
                    session.flushQueue();
                }

                plugin.getLogger().info("Pasted arena '" + config.getName() + "' at "
                        + center.getWorld().getName() + " " + center.getBlockX() + "," + center.getBlockY() + ","
                        + center.getBlockZ());

                // Complete the future on the MAIN THREAD so downstream code is thread-safe
                // and blocks have had time to sync to the world
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> future.complete(true),
                        10L); // 10 tick delay to let FAWE sync blocks to the world
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to paste arena: " + e.getMessage(), e);
                Bukkit.getScheduler().runTask(plugin, () -> future.complete(false));
            }
        });
        return future;
    }

    public void endSession(Fight fight) {
        FightSession s = activeSessions.remove(fight);

        if (s == null) {
            plugin.getLogger().warning("endSession called but no active session found for this fight.");
            return;
        }

        plugin.getLogger().info("Ending session for fight, clearing arena at "
                + s.getCenter().getWorld().getName() + " " + s.getCenter().getBlockX() + ","
                + s.getCenter().getBlockY() + "," + s.getCenter().getBlockZ());

        // Wait for a still-running paste to finish before clearing — otherwise the
        // clear runs first and the late paste leaves the arena stuck in the world.
        CompletableFuture<Boolean> paste = s.getPasteFuture();
        if (paste == null)
            paste = CompletableFuture.completedFuture(true);

        paste.whenComplete((ok, err) -> clearArenaAsync(s.getConfig(), s.getCenter()).thenRun(() -> {
            // Release chunk tickets and recycle the slot only AFTER the blocks
            // are actually cleared
            Bukkit.getScheduler().runTask(plugin, () -> {
                removeChunkTickets(s.getCenter());
                freeSlots.addLast(s.getCenter());
                plugin.getLogger().info("Released chunk tickets after arena clear.");
            });
        }));
    }

    private CompletableFuture<Void> clearArenaAsync(ArenaConfig config, Location center) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            clearArenaBlocks(config, center);
            future.complete(null);
        });
        return future;
    }

    private void clearArenaBlocks(ArenaConfig config, Location center) {
        try {
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(center.getWorld());

            Vector c1Offset = config.getCorner1();
            Vector c2Offset = config.getCorner2();
            Vector ctrOffset = config.getCenter();

            // Account for the center offset (same offset used during paste)
            int baseX = center.getBlockX() + ctrOffset.getBlockX();
            int baseY = center.getBlockY() + ctrOffset.getBlockY();
            int baseZ = center.getBlockZ() + ctrOffset.getBlockZ();

            BlockVector3 min = BlockVector3.at(
                    baseX + Math.min(c1Offset.getBlockX(), c2Offset.getBlockX()),
                    baseY + Math.min(c1Offset.getBlockY(), c2Offset.getBlockY()),
                    baseZ + Math.min(c1Offset.getBlockZ(), c2Offset.getBlockZ()));
            BlockVector3 max = BlockVector3.at(
                    baseX + Math.max(c1Offset.getBlockX(), c2Offset.getBlockX()),
                    baseY + Math.max(c1Offset.getBlockY(), c2Offset.getBlockY()),
                    baseZ + Math.max(c1Offset.getBlockZ(), c2Offset.getBlockZ()));

            plugin.getLogger()
                    .info("Clearing region from " + min + " to " + max + " in " + center.getWorld().getName());

            try (EditSession session = WorldEdit.getInstance().newEditSession(weWorld)) {
                session.setSideEffectApplier(SideEffectSet.none());
                CuboidRegion region = new CuboidRegion(weWorld, min, max);
                int affected = session.setBlocks((Region) region, BlockTypes.AIR.getDefaultState().toBaseBlock());
                session.flushQueue();
                plugin.getLogger().info("Cleared " + affected + " blocks.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to clear arena: " + e.getMessage(), e);
        }
    }

    public ArenaConfig getRandomArenaForKit(String kit) {
        List<ArenaConfig> valid = arenas.values().stream().filter(c -> c.isKitAllowed(kit)).toList();
        return valid.isEmpty() ? null : valid.get(random.nextInt(valid.size()));
    }

    public FightSession getSession(Fight fight) {
        return activeSessions.get(fight);
    }

    public Map<String, ArenaConfig> getArenas() {
        return new HashMap<>(arenas);
    }

    public void shutdown() {
        // The scheduler rejects new tasks while the plugin is disabling, so the
        // async endSession path can never run here — clear synchronously instead.
        for (FightSession s : activeSessions.values()) {
            clearArenaBlocks(s.getConfig(), s.getCenter());
            removeChunkTickets(s.getCenter());
        }
        activeSessions.clear();
    }

    public Arena getOrAllocateDynamicArena(boolean isBuild) {
        String baseName = isBuild ? "dynamicbuild" : "dynamic";
        List<Arena> existing = StrikePractice.getAPI().getArenas();

        // 1. Try to find a free existing arena of the SAME variant. Matching on a
        // bare "dynamic" prefix would also grab "dynamicbuild" arenas, handing
        // non-build fights a build arena.
        for (Arena a : existing) {
            String name = a.getName().toLowerCase();
            boolean sameVariant = name.equals(baseName) || name.startsWith(baseName + "_");
            if (!isBuild && name.startsWith("dynamicbuild"))
                sameVariant = false;
            if (sameVariant && !a.isUsing()) {
                return a;
            }
        }

        // 2. No free arena, create a new one
        // Find the base template (the one strictly named "dynamic" or "dynamicbuild")
        Arena template = StrikePractice.getAPI().getArena(baseName);
        if (template == null) {
            plugin.getLogger().warning("Base arena '" + baseName + "' missing! Cannot allocate dynamic arena.");
            return null;
        }

        // Find a new name
        int i = 1;
        String newName;
        do {
            i++;
            newName = baseName + "_" + i;
        } while (StrikePractice.getAPI().getArena(newName) != null);

        // Create it
        createBaseSpArena(newName, template.getLoc1(), isBuild); // Uses helper which saves it
        Arena newArena = StrikePractice.getAPI().getArena(newName);
        if (newArena != null) {
            // Ensure permissions/kits are copied from template in case createBaseSpArena
            // didn't (it clears kits)
            newArena.setKits(template.getKits());
            newArena.saveForStrikePractice();
            return newArena;
        }

        return null;
    }
}
