package net.eeebsiekat.wanderwood.glow.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public class GlowNode {
    private final int id;
    private final Vec3 pos;

    public GlowNode(int id, Vec3 pos) {
        this.id = id;
        this.pos = pos;
    }

    public int getId() { return id; }
    public Vec3 getPos() { return pos; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Id", id);
        tag.putDouble("X", pos.x);
        tag.putDouble("Y", pos.y);
        tag.putDouble("Z", pos.z);
        return tag;
    }

    public static GlowNode load(CompoundTag tag) {
        int id = tag.getInt("Id");
        Vec3 pos = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
        return new GlowNode(id, pos);
    }
}