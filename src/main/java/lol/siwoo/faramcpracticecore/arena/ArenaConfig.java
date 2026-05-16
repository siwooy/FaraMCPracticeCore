package lol.siwoo.faramcpracticecore.arena;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ArenaConfig {
    private final File file;
    private final String name;
    private final String schematicName;
    private Vector pos1, pos2, corner1, corner2, center;
    private final List<String> kits;

    public ArenaConfig(File file) {
        this.file = file;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.name = config.getString("name", file.getName().replace(".yml", ""));

        // FIXED: Removed "generated_" default to avoid accidental stone platforms
        this.schematicName = config.getString("schematic", this.name.toLowerCase() + ".schem");

        this.pos1 = config.getVector("pos1", new Vector(10, 5, 0));
        this.pos2 = config.getVector("pos2", new Vector(-10, 5, 0));
        this.corner1 = config.getVector("corner1", new Vector(30, 30, 30));
        this.corner2 = config.getVector("corner2", new Vector(-30, 0, -30));
        this.center = config.getVector("center", new Vector(0, 0, 0));

        this.kits = new ArrayList<>();
        List<String> list = config.getStringList("kits");
        if (list != null) for (String k : list) kits.add(k.toLowerCase());
    }

    public String getName() { return name; }
    public String getSchematicName() { return schematicName; }
    public Vector getPos1() { return pos1.clone(); }
    public Vector getPos2() { return pos2.clone(); }
    public Vector getCorner1() { return corner1.clone(); }
    public Vector getCorner2() { return corner2.clone(); }
    public Vector getCenter() { return center.clone(); }
    public boolean isKitAllowed(String kit) { return kits.isEmpty() || kits.contains(kit.toLowerCase()); }

    public void setPos1(Vector v) { this.pos1 = v; save(); }
    public void setPos2(Vector v) { this.pos2 = v; save(); }
    public void setCorner1(Vector v) { this.corner1 = v; save(); }
    public void setCorner2(Vector v) { this.corner2 = v; save(); }
    public void setCenter(Vector v) { this.center = v; save(); }

    private void save() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("pos1", pos1);
        config.set("pos2", pos2);
        config.set("corner1", corner1);
        config.set("corner2", corner2);
        config.set("center", center);
        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save arena config: " + file.getName(), e);
        }
    }
}