package lol.siwoo.faramcpracticecore.arena;

import ga.strikepractice.arena.Arena;
import ga.strikepractice.fights.Fight;
import org.bukkit.Location;

import java.util.concurrent.CompletableFuture;

public class FightSession {
    private final Fight fight;
    private final ArenaConfig config;
    private final Location center;
    private Arena spArena;
    private boolean dynamicallyCreated;
    // Completes when the schematic paste finishes; endSession must wait on this
    // so a fight that ends mid-paste doesn't clear the slot before the paste
    // lands
    private volatile CompletableFuture<Boolean> pasteFuture;

    public FightSession(Fight fight, ArenaConfig config, Location center) {
        this.fight = fight;
        this.config = config;
        this.center = center;
    }

    public Fight getFight() {
        return fight;
    }

    public ArenaConfig getConfig() {
        return config;
    }

    public Location getCenter() {
        return center;
    }

    public Arena getSpArena() {
        return spArena;
    }

    public void setSpArena(Arena spArena) {
        this.spArena = spArena;
    }

    public boolean isDynamicallyCreated() {
        return dynamicallyCreated;
    }

    public void setDynamicallyCreated(boolean dynamicallyCreated) {
        this.dynamicallyCreated = dynamicallyCreated;
    }

    public CompletableFuture<Boolean> getPasteFuture() {
        return pasteFuture;
    }

    public void setPasteFuture(CompletableFuture<Boolean> pasteFuture) {
        this.pasteFuture = pasteFuture;
    }
}