package net.eeebsiekat.wanderwood.glow.network;

import net.eeebsiekat.wanderwood.glow.data.GlowNetworkSavedData;
import net.eeebsiekat.wanderwood.glow.data.GlowWaypointSavedData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;

public record ServerboundRequestWaypointsPacket() implements CustomPacketPayload {

    public static final Type<ServerboundRequestWaypointsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("wanderwood", "request_waypoints"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundRequestWaypointsPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {},
            buf -> new ServerboundRequestWaypointsPacket()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(ServerboundRequestWaypointsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                GlowNetworkSavedData networkData = GlowNetworkSavedData.get(player.serverLevel());
                GlowWaypointSavedData waypointData = GlowWaypointSavedData.get(player.serverLevel());

                PacketDistributor.sendToPlayer(
                        player,
                        new ClientboundGlowSyncPacket(
                                new ArrayList<>(networkData.getNodes()),
                                networkData.getLines(),
                                new ArrayList<>(waypointData.getAllWaypoints())
                        )
                );
            }
        });
    }
}