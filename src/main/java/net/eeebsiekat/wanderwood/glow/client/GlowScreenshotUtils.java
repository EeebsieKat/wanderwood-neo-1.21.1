package net.eeebsiekat.wanderwood.glow.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
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

@EventBusSubscriber(value = Dist.CLIENT)
public class GlowScreenshotUtils {

    private static boolean capturing = false;
    private static int targetNodeId = -1;

    public static boolean isCapturing() {
        return capturing;
    }

    public static void captureNodeScreenshot(int nodeId) {
        targetNodeId = nodeId;
        capturing = true;
        Minecraft.getInstance().options.hideGui = true;
    }

    private static void saveScreenshot(int nodeId) {
        Minecraft mc = Minecraft.getInstance();
        Path dir = Paths.get(mc.gameDirectory.getPath(), "wanderwood", "nodes");
        File outputFile = new File(dir.toFile(), "node_" + nodeId + ".png");

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
            saveScreenshot(targetNodeId);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiLayerEvent.Pre event) {
        if (capturing) {
            event.setCanceled(true);
        }
    }
}