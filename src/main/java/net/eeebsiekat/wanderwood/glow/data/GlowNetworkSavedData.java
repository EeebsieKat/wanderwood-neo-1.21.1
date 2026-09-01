package net.eeebsiekat.wanderwood.glow.data;

import net.eeebsiekat.wanderwood.glow.gen.DelaunayTriangulation;
import net.eeebsiekat.wanderwood.glow.gen.PoissonDiscSampler;
import net.eeebsiekat.wanderwood.glow.network.GlowLine;
import net.eeebsiekat.wanderwood.glow.network.GlowNode;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class GlowNetworkSavedData extends SavedData {

    private final Map<Integer, GlowNode> nodes = new HashMap<>();
    private final List<GlowLine> lines = new ArrayList<>();
    private boolean initialized = false;

    public GlowNetworkSavedData() {}

    public static Factory<GlowNetworkSavedData> factory() {
        return new Factory<>(
                GlowNetworkSavedData::new,
                GlowNetworkSavedData::load,
                DataFixTypes.LEVEL
        );
    }

    public static GlowNetworkSavedData get(ServerLevel level) {
        GlowNetworkSavedData data = level.getDataStorage().computeIfAbsent(factory(), "wanderwood_glow_network");
        if (!data.initialized) {
            data.generateNetwork(level);
        }
        return data;
    }

    public void generateNetwork(ServerLevel level) {
        nodes.clear();
        lines.clear();

        double maxGenerationRadius = 2500.0;
        double borderSize = Math.min(level.getWorldBorder().getSize() / 2.0, maxGenerationRadius);
        double minRadius = 64.0;

        List<Vec2> points2D = PoissonDiscSampler.generatePoints(
                minRadius, -borderSize, -borderSize, borderSize, borderSize, 30, level.getRandom()
        );

        int idCounter = 0;
        Map<Vec2, GlowNode> pointToNodeMap = new HashMap<>();

        for (Vec2 p : points2D) {
            int blockX = (int) p.x;
            int blockZ = (int) p.y;
            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);

            Vec3 lockedPos = new Vec3(p.x, surfaceY + 5.0, p.y);
            GlowNode node = new GlowNode(idCounter++, lockedPos);

            nodes.put(node.getId(), node);
            pointToNodeMap.put(p, node);
        }

        Set<DelaunayTriangulation.Edge2D> edges = DelaunayTriangulation.triangulate(points2D);
        for (DelaunayTriangulation.Edge2D edge : edges) {
            GlowNode nodeA = pointToNodeMap.get(edge.p1());
            GlowNode nodeB = pointToNodeMap.get(edge.p2());

            if (nodeA != null && nodeB != null) {
                lines.add(new GlowLine(nodeA, nodeB));
            }
        }

        this.initialized = true;
        this.setDirty();
    }

    public List<GlowLine> getLines() { return lines; }
    public Collection<GlowNode> getNodes() { return nodes.values(); }

    public double getGlowStrengthAt(ServerLevel level, Vec3 point, double maxRadius) {
        double minDistance = Double.MAX_VALUE;

        for (GlowLine line : lines) {
            double dist = line.distanceToPoint(level, point);
            if (dist < minDistance) {
                minDistance = dist;
            }
        }

        double innerRadius = 2.0; // 100% field strength within 2 blocks of the line
        if (minDistance <= innerRadius) return 1.0;
        if (minDistance >= maxRadius) return 0.0;

        // Smooth cubic falloff for distances beyond the inner radius
        double linearFactor = 1.0 - ((minDistance - innerRadius) / (maxRadius - innerRadius));
        return Math.pow(linearFactor, 3);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("Initialized", initialized);

        ListTag nodeList = new ListTag();
        for (GlowNode node : nodes.values()) {
            nodeList.add(node.save());
        }
        tag.put("Nodes", nodeList);

        ListTag lineList = new ListTag();
        for (GlowLine line : lines) {
            lineList.add(line.save());
        }
        tag.put("Lines", lineList);

        return tag;
    }

    public static GlowNetworkSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        GlowNetworkSavedData data = new GlowNetworkSavedData();
        data.initialized = tag.getBoolean("Initialized");

        ListTag nodeList = tag.getList("Nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < nodeList.size(); i++) {
            GlowNode node = GlowNode.load(nodeList.getCompound(i));
            data.nodes.put(node.getId(), node);
        }

        ListTag lineList = tag.getList("Lines", Tag.TAG_COMPOUND);
        for (int i = 0; i < lineList.size(); i++) {
            CompoundTag lineTag = lineList.getCompound(i);
            GlowNode nodeA = data.nodes.get(lineTag.getInt("NodeA"));
            GlowNode nodeB = data.nodes.get(lineTag.getInt("NodeB"));

            if (nodeA != null && nodeB != null) {
                data.lines.add(new GlowLine(nodeA, nodeB));
            }
        }

        return data;
    }
}