package net.eeebsiekat.wanderwood.glow.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class Waypoint {

    public enum Type {
        SHROOME(50000.0, 0.5),
        STUMP(150000.0, 0.75),
        PERSONAL_SHROOME(0.0, 0.0);

        private final double maxGlow;
        private final double passiveRecovery;

        Type(double maxGlow, double passiveRecovery) {
            this.maxGlow = maxGlow;
            this.passiveRecovery = passiveRecovery;
        }

        public double getMaxGlow() { return maxGlow; }
        public double getPassiveRecovery() { return passiveRecovery; }
    }

    private final UUID id;
    private final UUID owner;
    private String name;
    private final BlockPos pos;
    private final ResourceKey<Level> dimension;
    private final Type type;
    private double storedGlow;

    public Waypoint(UUID id, UUID owner, String name, BlockPos pos, ResourceKey<Level> dimension, Type type, double storedGlow) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.pos = pos;
        this.dimension = dimension;
        this.type = type;
        this.storedGlow = storedGlow;
    }

    public UUID getId() { return id; }
    public UUID getOwner() { return owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BlockPos getPos() { return pos; }
    public ResourceKey<Level> getDimension() { return dimension; }
    public Type getType() { return type; }
    public double getStoredGlow() { return storedGlow; }
    public void setStoredGlow(double storedGlow) {
        this.storedGlow = Math.min(storedGlow, type.getMaxGlow());
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putUUID("Owner", owner);
        tag.putString("Name", name);
        tag.putInt("PosX", pos.getX());
        tag.putInt("PosY", pos.getY());
        tag.putInt("PosZ", pos.getZ());
        tag.putString("Dimension", dimension.location().toString());
        tag.putString("Type", type.name());
        tag.putDouble("StoredGlow", storedGlow);
        return tag;
    }

    public static Waypoint load(CompoundTag tag, HolderLookup.Provider registries) {
        UUID id = tag.getUUID("Id");
        UUID owner = tag.getUUID("Owner");
        String name = tag.getString("Name");
        BlockPos pos = new BlockPos(tag.getInt("PosX"), tag.getInt("PosY"), tag.getInt("PosZ"));
        ResourceKey<Level> dimension = ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.parse(tag.getString("Dimension"))
        );
        Type type = Type.valueOf(tag.getString("Type"));
        double storedGlow = tag.getDouble("StoredGlow");

        return new Waypoint(id, owner, name, pos, dimension, type, storedGlow);
    }
}