package net.eeebsiekat.wanderwood.glow.client.gui;

import net.eeebsiekat.wanderwood.glow.client.ClientGlowData;
import net.eeebsiekat.wanderwood.glow.client.GlowScreenshotUtils;
import net.eeebsiekat.wanderwood.glow.data.Waypoint;
import net.eeebsiekat.wanderwood.glow.network.ServerboundGlowTravelPacket;
import net.eeebsiekat.wanderwood.glow.network.ServerboundRequestWaypointsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

public class GlowWaypointScreen extends Screen {

    private static final double TELEPORT_COST = 12500.0;
    private UUID selectedWaypointId = null;
    private Button travelButton;
    private int tickCounter = 0;

    public GlowWaypointScreen() {
        super(Component.literal("Glow Waypoint Network"));
    }

    @Override
    protected void init() {
        super.init();

        // Initial sync request on screen open
        PacketDistributor.sendToServer(new ServerboundRequestWaypointsPacket());

        this.travelButton = this.addRenderableWidget(
                Button.builder(Component.literal("Travel"), b -> {
                            Waypoint selected = getSelectedWaypoint();
                            if (selected != null) {
                                PacketDistributor.sendToServer(
                                        new ServerboundGlowTravelPacket(
                                                selected.getPos().getBottomCenter(),
                                                selected.getDimension()
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
    public void tick() {
        super.tick();
        tickCounter++;
        // Request live data from server every 10 ticks (twice per second)
        if (tickCounter % 10 == 0) {
            PacketDistributor.sendToServer(new ServerboundRequestWaypointsPacket());
        }
    }

    private Waypoint getSelectedWaypoint() {
        if (selectedWaypointId == null) return null;
        return ClientGlowData.getWaypoints().stream()
                .filter(wp -> wp.getId().equals(selectedWaypointId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        List<Waypoint> waypoints = ClientGlowData.getWaypoints();
        Waypoint selectedWaypoint = getSelectedWaypoint();

        int listX = 20;
        int listY = 40;
        int itemHeight = 22;

        guiGraphics.drawString(this.font, "Waypoints (" + waypoints.size() + "):", listX, listY - 12, 0xAAAAAA);

        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint wp = waypoints.get(i);
            int currentY = listY + (i * itemHeight);

            boolean isSelected = wp.getId().equals(selectedWaypointId);
            int nameColor = isSelected ? 0xFFFF00 : 0xFFFFFF;

            // Green if >= 12,500 Glow; Red if under
            int glowColor = wp.getStoredGlow() >= TELEPORT_COST ? 0x55FF55 : 0xFF5555;

            guiGraphics.drawString(this.font, wp.getName(), listX + 5, currentY + 4, nameColor);
            guiGraphics.drawString(this.font, String.format("%.0f Glow", wp.getStoredGlow()), listX + 130, currentY + 4, glowColor);
        }

        if (selectedWaypoint != null) {
            int detailsX = this.width / 2 + 20;
            int detailsY = 40;

            boolean hasEnoughGlow = selectedWaypoint.getStoredGlow() >= TELEPORT_COST;
            int costColor = hasEnoughGlow ? 0x55FF55 : 0xFF5555;

            guiGraphics.drawString(this.font, "Name: " + selectedWaypoint.getName(), detailsX, detailsY, 0xFFFFFF);
            guiGraphics.drawString(this.font, "Pos: " + selectedWaypoint.getPos().toShortString(), detailsX, detailsY + 15, 0xAAAAAA);
            guiGraphics.drawString(this.font, String.format("Cost: %.0f Glow", TELEPORT_COST), detailsX, detailsY + 30, costColor);

            if (travelButton != null) {
                travelButton.active = hasEnoughGlow;
            }

            ResourceLocation preview = GlowScreenshotUtils.getOrLoadScreenshot(selectedWaypoint.getId());
            if (preview != null) {
                guiGraphics.blit(preview, detailsX, detailsY + 50, 0, 0, 120, 90, 120, 90);
            } else {
                guiGraphics.drawString(this.font, "[ No Preview Image ]", detailsX, detailsY + 60, 0x888888);
            }
        } else if (travelButton != null) {
            travelButton.active = false;
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
                selectedWaypointId = waypoints.get(i).getId();
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