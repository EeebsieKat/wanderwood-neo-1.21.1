package net.eeebsiekat.wanderwood.glow.block.entity;

import net.eeebsiekat.wanderwood.glow.data.GlowNetworkSavedData;
import net.eeebsiekat.wanderwood.glow.data.GlowWaypointSavedData;
import net.eeebsiekat.wanderwood.glow.data.Waypoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public abstract class AbstractWaypointBlockEntity extends BlockEntity {

    protected double storedGlow = 0.0;
    protected double currentFieldStrength = 0.0;
    protected UUID waypointId = null;
    protected UUID ownerId = null;
    protected String customName = "";

    public AbstractWaypointBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract double getMaxGlow();
    public abstract Waypoint.Type getWaypointType();
    public abstract boolean canGenerateGlow(ServerLevel level, BlockPos pos);

    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractWaypointBlockEntity entity) {
        if (level instanceof ServerLevel serverLevel && level.getGameTime() % 20 == 0) {
            // Register with saved data if not registered yet
            if (entity.waypointId == null && entity.ownerId != null) {
                GlowWaypointSavedData waypointData = GlowWaypointSavedData.get(serverLevel);
                Waypoint wp = waypointData.registerWaypoint(
                        entity.ownerId,
                        entity.customName.isEmpty() ? entity.getWaypointType().name() : entity.customName,
                        pos,
                        serverLevel.dimension(),
                        entity.getWaypointType()
                );
                entity.waypointId = wp.getId();
                entity.setChanged();
            }

            // Glow extraction logic
            if (entity.canGenerateGlow(serverLevel, pos)) {
                GlowNetworkSavedData networkData = GlowNetworkSavedData.get(serverLevel);
                Vec3 centerPos = pos.getCenter();

                entity.currentFieldStrength = networkData.getGlowStrengthAt(serverLevel, centerPos, 32.0);
                entity.storedGlow = Math.min(
                        entity.getMaxGlow(),
                        entity.storedGlow + (entity.currentFieldStrength * 10.0)
                );
            } else {
                entity.currentFieldStrength = 0.0;
            }

            // Sync with global waypoint data if registered
            if (entity.waypointId != null) {
                GlowWaypointSavedData waypointData = GlowWaypointSavedData.get(serverLevel);
                waypointData.getWaypoint(entity.waypointId).ifPresent(wp -> wp.setStoredGlow(entity.storedGlow));
            }

            entity.setChanged();
        }
    }

    public void unregisterWaypoint(ServerLevel level) {
        if (waypointId != null) {
            GlowWaypointSavedData.get(level).removeWaypoint(waypointId);
        }
    }

    public void setOwner(UUID ownerId) {
        this.ownerId = ownerId;
        setChanged();
    }

    public UUID getWaypointId() {
        return waypointId;
    }

    public void setWaypointId(UUID waypointId) {
        this.waypointId = waypointId;
        setChanged();
    }

    public double getCurrentFieldStrength() { return currentFieldStrength; }
    public double getStoredGlow() { return storedGlow; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("StoredGlow", storedGlow);
        tag.putDouble("FieldStrength", currentFieldStrength);
        if (waypointId != null) tag.putUUID("WaypointId", waypointId);
        if (ownerId != null) tag.putUUID("OwnerId", ownerId);
        tag.putString("CustomName", customName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.storedGlow = tag.getDouble("StoredGlow");
        this.currentFieldStrength = tag.getDouble("FieldStrength");
        if (tag.hasUUID("WaypointId")) this.waypointId = tag.getUUID("WaypointId");
        if (tag.hasUUID("OwnerId")) this.ownerId = tag.getUUID("OwnerId");
        this.customName = tag.getString("CustomName");
    }
}