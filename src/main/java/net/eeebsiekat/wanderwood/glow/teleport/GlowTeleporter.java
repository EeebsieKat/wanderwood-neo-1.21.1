package net.eeebsiekat.wanderwood.glow.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

public class GlowTeleporter {

    public static Vec3 findSafePosition(BlockPos pos, Level level) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos checkPos = pos.relative(dir);
            BlockState state = level.getBlockState(checkPos);
            // Call on state.getBlock() instead of state directly
            if (state.getBlock().isPossibleToRespawnInThis(state)) {
                return new Vec3(checkPos.getX() + 0.5, checkPos.getY() + 0.1, checkPos.getZ() + 0.5);
            }
        }
        return new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
    }

    public static void travelToNode(ServerPlayer player, Vec3 destination, ResourceKey<Level> targetDimension) {
        ServerLevel currentLevel = player.serverLevel();
        BlockPos destBlockPos = BlockPos.containing(destination);

        currentLevel.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

        if (!currentLevel.dimension().equals(targetDimension)) {
            ServerLevel targetLevel = currentLevel.getServer().getLevel(targetDimension);
            if (targetLevel != null) {
                Vec3 safePos = findSafePosition(destBlockPos, targetLevel);
                DimensionTransition transition = new DimensionTransition(
                        targetLevel,
                        safePos,
                        Vec3.ZERO,
                        player.getYRot(),
                        player.getXRot(),
                        false,
                        DimensionTransition.DO_NOTHING
                );
                player.changeDimension(transition);
            }
        } else {
            Vec3 safePos = findSafePosition(destBlockPos, currentLevel);
            player.connection.teleport(safePos.x, safePos.y, safePos.z, player.getYRot(), player.getXRot());
        }

        player.serverLevel().playSound(null, destBlockPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}