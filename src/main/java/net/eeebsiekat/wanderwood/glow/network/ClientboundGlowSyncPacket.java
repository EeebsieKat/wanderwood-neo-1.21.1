package net.eeebsiekat.wanderwood.glow.network;

import net.eeebsiekat.wanderwood.TheWanderwood;
import net.eeebsiekat.wanderwood.glow.client.ClientGlowData;
import net.eeebsiekat.wanderwood.glow.data.Waypoint;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ClientboundGlowSyncPacket(
        List<GlowNode> nodes,
        List<GlowLine> lines,
        List<Waypoint> waypoints
) implements CustomPacketPayload {

    public static final Type<ClientboundGlowSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TheWanderwood.MODID, "glow_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundGlowSyncPacket> STREAM_CODEC =
            CustomPacketPayload.codec(
                    ClientboundGlowSyncPacket::write,
                    ClientboundGlowSyncPacket::new
            );

    public ClientboundGlowSyncPacket(RegistryFriendlyByteBuf buf) {
        this(readNodes(buf), readLines(buf), readWaypoints(buf));
    }

    private static List<GlowNode> readNodes(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<GlowNode> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new GlowNode(buf.readInt(), new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())));
        }
        return list;
    }

    private static List<GlowLine> readLines(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<GlowLine> list = new ArrayList<>(count);
        Map<Integer, GlowNode> nodeMap = new HashMap<>();
        for (GlowNode node : ClientGlowData.getNodes()) {
            nodeMap.put(node.getId(), node);
        }
        for (int i = 0; i < count; i++) {
            GlowNode start = nodeMap.get(buf.readInt());
            GlowNode end = nodeMap.get(buf.readInt());
            if (start != null && end != null) {
                list.add(new GlowLine(start, end));
            }
        }
        return list;
    }

    private static List<Waypoint> readWaypoints(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Waypoint> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new Waypoint(
                    buf.readUUID(),
                    buf.readUUID(),
                    buf.readUtf(),
                    buf.readBlockPos(),
                    buf.readResourceKey(Registries.DIMENSION),
                    buf.readEnum(Waypoint.Type.class),
                    buf.readDouble()
            ));
        }
        return list;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(nodes.size());
        for (GlowNode node : nodes) {
            buf.writeInt(node.getId());
            buf.writeDouble(node.getPos().x);
            buf.writeDouble(node.getPos().y);
            buf.writeDouble(node.getPos().z);
        }

        buf.writeVarInt(lines.size());
        for (GlowLine line : lines) {
            buf.writeInt(line.getStart().getId());
            buf.writeInt(line.getEnd().getId());
        }

        buf.writeVarInt(waypoints.size());
        for (Waypoint wp : waypoints) {
            buf.writeUUID(wp.getId());
            buf.writeUUID(wp.getOwner());
            buf.writeUtf(wp.getName());
            buf.writeBlockPos(wp.getPos());
            buf.writeResourceKey(wp.getDimension());
            buf.writeEnum(wp.getType());
            buf.writeDouble(wp.getStoredGlow());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handleClient(ClientboundGlowSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientGlowData.setNetwork(packet.nodes(), packet.lines());
            ClientGlowData.setWaypoints(packet.waypoints());
        });
    }
}