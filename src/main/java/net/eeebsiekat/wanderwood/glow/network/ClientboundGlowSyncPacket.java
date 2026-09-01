package net.eeebsiekat.wanderwood.glow.network;

import net.eeebsiekat.wanderwood.glow.client.ClientGlowData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record ClientboundGlowSyncPacket(List<GlowNode> nodes, List<GlowLine> lines) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientboundGlowSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("wanderwood", "glow_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundGlowSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.nodes().size());
                for (GlowNode node : packet.nodes()) {
                    buf.writeVarInt(node.getId());
                    buf.writeDouble(node.getPos().x);
                    buf.writeDouble(node.getPos().y);
                    buf.writeDouble(node.getPos().z);
                }

                buf.writeVarInt(packet.lines().size());
                for (GlowLine line : packet.lines()) {
                    buf.writeVarInt(line.getStart().getId());
                    buf.writeVarInt(line.getEnd().getId());
                }
            },
            buf -> {
                int nodeCount = buf.readVarInt();
                List<GlowNode> nodes = new ArrayList<>(nodeCount);
                for (int i = 0; i < nodeCount; i++) {
                    int id = buf.readVarInt();
                    Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                    nodes.add(new GlowNode(id, pos));
                }

                int lineCount = buf.readVarInt();
                List<GlowLine> lines = new ArrayList<>(lineCount);
                for (int i = 0; i < lineCount; i++) {
                    int nodeAId = buf.readVarInt();
                    int nodeBId = buf.readVarInt();
                    GlowNode nodeA = nodes.stream().filter(n -> n.getId() == nodeAId).findFirst().orElse(null);
                    GlowNode nodeB = nodes.stream().filter(n -> n.getId() == nodeBId).findFirst().orElse(null);
                    if (nodeA != null && nodeB != null) {
                        lines.add(new GlowLine(nodeA, nodeB));
                    }
                }

                return new ClientboundGlowSyncPacket(nodes, lines);
            }
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(ClientboundGlowSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientGlowData.setNetwork(packet.nodes(), packet.lines()));
    }
}