package lol.siwoo.faramcpracticecore.train.npcs;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import lol.siwoo.faramcpracticecore.FaraMCPracticeCore;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.UUID;
import java.util.logging.Level;

public class TrainingNPC {
    private final FaraMCPracticeCore plugin;
    private final int entityId;
    private final UUID uuid;
    private final String name;
    private Location location;
    private boolean removed = false;

    public TrainingNPC(FaraMCPracticeCore plugin, Location location, String name) {
        this.plugin = plugin;
        this.entityId = (int) (Math.random() * Integer.MAX_VALUE);
        this.uuid = UUID.randomUUID();
        this.name = name;
        this.location = location.clone();
    }

    public void spawnForPlayer(Player player) {
        try {
            // Create player info packet
            PacketContainer playerInfoPacket = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.PLAYER_INFO);

            playerInfoPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);

            WrappedGameProfile profile = new WrappedGameProfile(uuid, name);
            PlayerInfoData data = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL,
                    WrappedChatComponent.fromText(name));

            playerInfoPacket.getPlayerInfoDataLists().write(0, Collections.singletonList(data));

            // Create spawn packet
            PacketContainer spawnPacket = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);

            spawnPacket.getIntegers().write(0, entityId);
            spawnPacket.getUUIDs().write(0, uuid);
            spawnPacket.getDoubles().write(0, location.getX());
            spawnPacket.getDoubles().write(1, location.getY());
            spawnPacket.getDoubles().write(2, location.getZ());
            spawnPacket.getBytes().write(0, (byte) ((location.getYaw() * 256.0F) / 360.0F));
            spawnPacket.getBytes().write(1, (byte) ((location.getPitch() * 256.0F) / 360.0F));

            // Send packets
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, playerInfoPacket);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, spawnPacket);

            // Remove from tab list after a short delay
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                try {
                    PacketContainer removeInfoPacket = ProtocolLibrary.getProtocolManager()
                            .createPacket(PacketType.Play.Server.PLAYER_INFO);

                    removeInfoPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
                    removeInfoPacket.getPlayerInfoDataLists().write(0, Collections.singletonList(data));

                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, removeInfoPacket);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error in TrainingNPC (delayed task)", e);
                }
            }, 5L);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error in TrainingNPC spawnForPlayer", e);
        }
    }

    public void removeForPlayer(Player player) {
        try {
            PacketContainer destroyPacket = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.ENTITY_DESTROY);

            destroyPacket.getIntegerArrays().write(0, new int[]{entityId});

            ProtocolLibrary.getProtocolManager().sendServerPacket(player, destroyPacket);
            removed = true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error in TrainingNPC removeForPlayer", e);
        }
    }

    public void teleportForPlayer(Player player, Location newLocation) {
        this.location = newLocation.clone();

        try {
            PacketContainer teleportPacket = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.ENTITY_TELEPORT);

            teleportPacket.getIntegers().write(0, entityId);
            teleportPacket.getDoubles().write(0, newLocation.getX());
            teleportPacket.getDoubles().write(1, newLocation.getY());
            teleportPacket.getDoubles().write(2, newLocation.getZ());
            teleportPacket.getBytes().write(0, (byte) ((newLocation.getYaw() * 256.0F) / 360.0F));
            teleportPacket.getBytes().write(1, (byte) ((newLocation.getPitch() * 256.0F) / 360.0F));
            teleportPacket.getBooleans().write(0, true);

            ProtocolLibrary.getProtocolManager().sendServerPacket(player, teleportPacket);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error in TrainingNPC teleportForPlayer", e);
        }
    }

    // Getters
    public int getEntityId() {
        return entityId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location.clone();
    }

    public boolean isRemoved() {
        return removed;
    }
}