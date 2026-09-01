package net.eeebsiekat.wanderwood.glow.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class GlowLine {
    private final GlowNode start;
    private final GlowNode end;

    public GlowLine(GlowNode start, GlowNode end) {
        this.start = start;
        this.end = end;
    }

    public GlowNode getStart() { return start; }
    public GlowNode getEnd() { return end; }

    public double distanceToPoint(Level level, Vec3 point) {
        Vec3 a = start.getPos();
        Vec3 b = end.getPos();

        double dx = b.x - a.x;
        double dz = b.z - a.z;
        double dist = Math.hypot(dx, dz);

        int steps = Math.max(1, (int) Math.ceil(dist / 2.0));

        double minDistSq = Double.MAX_VALUE;
        Vec3 prevPoint = getTerrainPoint(level, a.x, a.z);

        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double currentX = a.x + dx * t;
            double currentZ = a.z + dz * t;

            Vec3 currentPoint = getTerrainPoint(level, currentX, currentZ);

            double distSq = distToSegmentSq(point, prevPoint, currentPoint);
            if (distSq < minDistSq) {
                minDistSq = distSq;
            }

            prevPoint = currentPoint;
        }

        return Math.sqrt(minDistSq);
    }

    private Vec3 getTerrainPoint(Level level, double x, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
        return new Vec3(x, surfaceY + 5.0, z);
    }

    private double distToSegmentSq(Vec3 p, Vec3 a, Vec3 b) {
        double l2 = a.distanceToSqr(b);
        if (l2 == 0) return p.distanceToSqr(a);
        double t = Math.max(0, Math.min(1, ((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y) + (p.z - a.z) * (b.z - a.z)) / l2));
        Vec3 proj = new Vec3(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y), a.z + t * (b.z - a.z));
        return p.distanceToSqr(proj);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("NodeA", start.getId());
        tag.putInt("NodeB", end.getId());
        return tag;
    }
}