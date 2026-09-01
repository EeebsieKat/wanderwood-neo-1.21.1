package net.eeebsiekat.wanderwood.glow.block.entity;

import com.mojang.logging.LogUtils;
import net.eeebsiekat.wanderwood.TheWanderwood;
import net.eeebsiekat.wanderwood.glow.data.GlowNetworkSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class GlowExtractorBlockEntity extends BlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    private double storedGlow = 0.0;
    private double currentFieldStrength = 0.0;

    public GlowExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(TheWanderwood.GLOW_EXTRACTOR_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GlowExtractorBlockEntity entity) {
        if (level instanceof ServerLevel serverLevel && level.getGameTime() % 20 == 0) {
            GlowNetworkSavedData data = GlowNetworkSavedData.get(serverLevel);
            Vec3 centerPos = pos.getCenter();

            entity.currentFieldStrength = data.getGlowStrengthAt(serverLevel, centerPos, 32.0);
            entity.storedGlow += entity.currentFieldStrength * 10.0;

            LOGGER.info("[Glow Extractor at {}] Field Strength: {} | Stored Glow: {}",
                    pos.toShortString(),
                    String.format("%.2f", entity.currentFieldStrength),
                    String.format("%.1f", entity.storedGlow));

            entity.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("StoredGlow", storedGlow);
        tag.putDouble("FieldStrength", currentFieldStrength);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.storedGlow = tag.getDouble("StoredGlow");
        this.currentFieldStrength = tag.getDouble("FieldStrength");
    }

    public double getCurrentFieldStrength() { return currentFieldStrength; }
    public double getStoredGlow() { return storedGlow; }
}