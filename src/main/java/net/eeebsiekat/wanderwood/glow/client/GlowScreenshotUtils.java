package net.eeebsiekat.wanderwood.glow.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.eeebsiekat.wanderwood.TheWanderwood;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(value = Dist.CLIENT)
public class GlowScreenshotUtils {

    private static final Map<UUID, ResourceLocation> SCREENSHOT_CACHE = new HashMap<>();
    private static boolean capturing = false;
    private static UUID targetWaypointId = null;

    public static boolean isCapturing() {
        return capturing;
    }

    public static void captureNodeScreenshot(UUID waypointId) {
        targetWaypointId = waypointId;
        capturing = true;
        Minecraft.getInstance().options.hideGui = true;
    }

    public static ResourceLocation getOrLoadScreenshot(UUID waypointId) {
        if (waypointId == null) return null;
        if (SCREENSHOT_CACHE.containsKey(waypointId)) {
            return SCREENSHOT_CACHE.get(waypointId);
        }

        Minecraft mc = Minecraft.getInstance();
        Path dir = Paths.get(mc.gameDirectory.getPath(), "wanderwood", "nodes");
        File file = new File(dir.toFile(), "node_" + waypointId + ".png");

        if (!file.exists()) {
            return null;
        }

        try (NativeImage image = NativeImage.read(Files.newInputStream(file.toPath()))) {
            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    TheWanderwood.MODID, "waypoint_preview_" + waypointId.toString().toLowerCase()
            );
            mc.getTextureManager().register(location, texture);
            SCREENSHOT_CACHE.put(waypointId, location);
            return location;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void saveScreenshot(UUID waypointId) {
        Minecraft mc = Minecraft.getInstance();
        Path dir = Paths.get(mc.gameDirectory.getPath(), "wanderwood", "nodes");
        File outputFile = new File(dir.toFile(), "node_" + waypointId + ".png");

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        try {
            image.writeToFile(outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            image.close();
        }
    }

    @SubscribeEvent
    public static void onRenderFramePost(RenderFrameEvent.Post event) {
        if (capturing && Minecraft.getInstance().level != null) {
            capturing = false;
            Minecraft.getInstance().options.hideGui = false;
            if (targetWaypointId != null) {
                saveScreenshot(targetWaypointId);
                targetWaypointId = null;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiLayerEvent.Pre event) {
        if (capturing) {
            event.setCanceled(true);
        }
    }
}