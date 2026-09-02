package net.eeebsiekat.wanderwood.glow.client.gui;

import net.eeebsiekat.wanderwood.glow.client.ClientGlowData;
import net.eeebsiekat.wanderwood.glow.client.GlowScreenshotUtils;
import net.eeebsiekat.wanderwood.glow.data.Waypoint;
import net.eeebsiekat.wanderwood.glow.network.ServerboundGlowTravelPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class GlowWaypointScreen extends Screen {

    private Waypoint selectedWaypoint = null;
    private Button travelButton;

    public GlowWaypointScreen() {
        super(Component.literal("Glow Waypoint Network"));
    }

    @Override
    protected void init() {
        super.init();

        this.travelButton = this.addRenderableWidget(
                Button.builder(Component.literal("Travel"), b -> {
                            if (selectedWaypoint != null) {
                                PacketDistributor.sendToServer(
                                        new ServerboundGlowTravelPacket(
                                                selectedWaypoint.getPos().getBottomCenter(),
                                                selectedWaypoint.getDimension()
                                        )
                                );
                                this.onClose();
                            }
                        })
                        .bounds(this.width / 2 + 20, this.height - 40, 100, 20)
                        .build()
        );
        this.travelButton.active = false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Direct fill bypasses Minecraft 1.21's screen blur shader pipeline completely
        guiGraphics.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Render background and widgets first
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 2. Render Screen Header
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        // 3. Render Waypoint List Section
        List<Waypoint> waypoints = ClientGlowData.getWaypoints();
        int listX = 20;
        int listY = 40;
        int itemHeight = 22;

        guiGraphics.drawString(this.font, "Waypoints (" + waypoints.size() + "):", listX, listY - 12, 0xAAAAAA);

        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint wp = waypoints.get(i);
            int currentY = listY + (i * itemHeight);

            boolean isSelected = wp.equals(selectedWaypoint);
            int color = isSelected ? 0xFFFF00 : 0xFFFFFF;

            guiGraphics.drawString(this.font, wp.getName(), listX + 5, currentY + 4, color);
            guiGraphics.drawString(this.font, String.format("%.0f Glow", wp.getStoredGlow()), listX + 130, currentY + 4, 0x55FF55);
        }

        // 4. Render Details & Screenshot Preview Section
        if (selectedWaypoint != null) {
            int detailsX = this.width / 2 + 20;
            int detailsY = 40;

            guiGraphics.drawString(this.font, "Name: " + selectedWaypoint.getName(), detailsX, detailsY, 0xFFFFFF);
            guiGraphics.drawString(this.font, "Pos: " + selectedWaypoint.getPos().toShortString(), detailsX, detailsY + 15, 0xAAAAAA);
            guiGraphics.drawString(this.font, "Type: " + selectedWaypoint.getType(), detailsX, detailsY + 30, 0xAAAAAA);

            ResourceLocation preview = GlowScreenshotUtils.getOrLoadScreenshot(selectedWaypoint.getId());
            if (preview != null) {
                guiGraphics.blit(preview, detailsX, detailsY + 50, 0, 0, 120, 90, 120, 90);
            } else {
                guiGraphics.drawString(this.font, "[ No Preview Image ]", detailsX, detailsY + 60, 0x888888);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Waypoint> waypoints = ClientGlowData.getWaypoints();
        int listX = 20;
        int listY = 40;
        int itemHeight = 22;

        for (int i = 0; i < waypoints.size(); i++) {
            int currentY = listY + (i * itemHeight);
            if (mouseX >= listX && mouseX <= listX + 200 && mouseY >= currentY && mouseY < currentY + itemHeight) {
                selectedWaypoint = waypoints.get(i);
                if (travelButton != null) {
                    travelButton.active = true;
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}