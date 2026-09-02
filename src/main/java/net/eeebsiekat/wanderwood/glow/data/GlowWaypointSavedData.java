package net.eeebsiekat.wanderwood.glow.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class GlowWaypointSavedData extends SavedData {

    private final Map<UUID, Waypoint> waypoints = new HashMap<>();

    public GlowWaypointSavedData() {}

    public static Factory<GlowWaypointSavedData> factory() {
        return new Factory<>(
                GlowWaypointSavedData::new,
                GlowWaypointSavedData::load,
                DataFixTypes.LEVEL
        );
    }

    public static GlowWaypointSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld level not initialized when accessing GlowWaypointSavedData.");
        }
        return overworld.getDataStorage().computeIfAbsent(factory(), "wanderwood_waypoints");
    }

    public Waypoint registerWaypoint(UUID owner, String name, BlockPos pos, ResourceKey<Level> dimension, Waypoint.Type type) {
        UUID id = UUID.randomUUID();
        Waypoint waypoint = new Waypoint(id, owner, name, pos, dimension, type, 0.0);
        waypoints.put(id, waypoint);
        setDirty();
        return waypoint;
    }

    public boolean removeWaypoint(UUID id) {
        if (waypoints.remove(id) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    public Optional<Waypoint> getWaypoint(UUID id) {
        return Optional.ofNullable(waypoints.get(id));
    }

    public Collection<Waypoint> getAllWaypoints() {
        return Collections.unmodifiableCollection(waypoints.values());
    }

    public List<Waypoint> getWaypointsForPlayer(UUID playerUUID) {
        return waypoints.values().stream()
                .filter(w -> w.getOwner().equals(playerUUID))
                .toList();
    }

    public int getConnectedCount() {
        return waypoints.size();
    }

    public int calculateTeleportCost() {
        int connected = getConnectedCount();
        return Math.max(0, 12500 - (connected * 625));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Waypoint waypoint : waypoints.values()) {
            list.add(waypoint.save(registries));
        }
        tag.put("Waypoints", list);
        return tag;
    }

    public static GlowWaypointSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        GlowWaypointSavedData data = new GlowWaypointSavedData();
        ListTag list = tag.getList("Waypoints", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag waypointTag = list.getCompound(i);
            Waypoint waypoint = Waypoint.load(waypointTag, registries);
            data.waypoints.put(waypoint.getId(), waypoint);
        }

        return data;
    }

    public static GlowWaypointSavedData get(ServerLevel level) {
        return get(level.getServer());
    }
}