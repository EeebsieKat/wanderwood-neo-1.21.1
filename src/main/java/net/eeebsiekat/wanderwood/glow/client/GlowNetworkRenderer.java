package net.eeebsiekat.wanderwood.glow.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.eeebsiekat.wanderwood.glow.network.GlowLine;
import net.eeebsiekat.wanderwood.glow.network.GlowNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = "wanderwood", value = Dist.CLIENT)
public class GlowNetworkRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (mc.player.getItemBySlot(EquipmentSlot.HEAD).getItem() != net.eeebsiekat.wanderwood.TheWanderwood.GLOW_GOGGLES.get()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        // Draw Terrain-Conforming Lines (Cyan)
        for (GlowLine line : ClientGlowData.getLines()) {
            Vec3 a = line.getStart().getPos();
            Vec3 b = line.getEnd().getPos();

            double dx = b.x - a.x;
            double dz = b.z - a.z;
            double dist = Math.hypot(dx, dz);

            // Subdivide line into ~2 block sub-segments
            int steps = Math.max(1, (int) Math.ceil(dist / 2.0));

            Vec3 prevPoint = getTerrainPoint(mc, a.x, a.z);

            for (int i = 1; i <= steps; i++) {
                double t = (double) i / steps;
                double currentX = a.x + dx * t;
                double currentZ = a.z + dz * t;

                Vec3 currentPoint = getTerrainPoint(mc, currentX, currentZ);

                consumer.addVertex(poseStack.last().pose(), (float) prevPoint.x, (float) prevPoint.y, (float) prevPoint.z)
                        .setColor(0, 255, 255, 255)
                        .setNormal(poseStack.last(), 0, 1, 0);

                consumer.addVertex(poseStack.last().pose(), (float) currentPoint.x, (float) currentPoint.y, (float) currentPoint.z)
                        .setColor(0, 255, 255, 255)
                        .setNormal(poseStack.last(), 0, 1, 0);

                prevPoint = currentPoint;
            }
        }

        // Draw Node Pillars (Gold)
        for (GlowNode node : ClientGlowData.getNodes()) {
            Vec3 rawPos = node.getPos();
            Vec3 pos = getTerrainPoint(mc, rawPos.x, rawPos.z);

            consumer.addVertex(poseStack.last().pose(), (float) pos.x, (float) pos.y - 5.0f, (float) pos.z)
                    .setColor(255, 215, 0, 255)
                    .setNormal(poseStack.last(), 0, 1, 0);

            consumer.addVertex(poseStack.last().pose(), (float) pos.x, (float) pos.y + 5.0f, (float) pos.z)
                    .setColor(255, 215, 0, 255)
                    .setNormal(poseStack.last(), 0, 1, 0);
        }

        poseStack.popPose();
        buffer.endBatch(RenderType.lines());
    }

    private static Vec3 getTerrainPoint(Minecraft mc, double x, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);

        int surfaceY = mc.level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
        return new Vec3(x, surfaceY + 5.0, z);
    }
}