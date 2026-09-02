package net.eeebsiekat.wanderwood.glow.network;

import net.eeebsiekat.wanderwood.glow.teleport.GlowTeleporter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundGlowTravelPacket(Vec3 targetPos, ResourceKey<Level> targetDimension) implements CustomPacketPayload {

    public static final Type<ServerboundGlowTravelPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("wanderwood", "glow_travel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundGlowTravelPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeDouble(packet.targetPos().x);
                buf.writeDouble(packet.targetPos().y);
                buf.writeDouble(packet.targetPos().z);
                buf.writeResourceKey(packet.targetDimension());
            },
            buf -> new ServerboundGlowTravelPacket(
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readResourceKey(Registries.DIMENSION)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(ServerboundGlowTravelPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                GlowTeleporter.travelToNode(player, packet.targetPos(), packet.targetDimension());
            }
        });
    }
}