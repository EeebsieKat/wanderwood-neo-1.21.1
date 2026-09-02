package net.eeebsiekat.wanderwood.glow.block;

import net.eeebsiekat.wanderwood.TheWanderwood;
import net.eeebsiekat.wanderwood.glow.block.entity.AbstractWaypointBlockEntity;
import net.eeebsiekat.wanderwood.glow.block.entity.StumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import net.eeebsiekat.wanderwood.glow.data.GlowNetworkSavedData;
import net.eeebsiekat.wanderwood.glow.data.GlowWaypointSavedData;
import net.eeebsiekat.wanderwood.glow.network.ClientboundGlowSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;

public class StumpBlock extends Block implements EntityBlock {

    public StumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer != null) {
            if (level.getBlockEntity(pos) instanceof AbstractWaypointBlockEntity be) {
                be.setOwner(placer.getUUID());
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isCrouching()) {
            if (!level.isClientSide()) {
                if (level.getBlockEntity(pos) instanceof StumpBlockEntity stump) {
                    boolean canSeeSky = level.canSeeSky(pos.above());
                    player.displayClientMessage(Component.literal(String.format(
                            "Stump | Sky Access: %s | Field Strength: %.2f | Stored Glow: %.1f / %.1f",
                            canSeeSky ? "Clear" : "Blocked",
                            stump.getCurrentFieldStrength(),
                            stump.getStoredGlow(),
                            stump.getMaxGlow()
                    )), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = (ServerLevel) level;
            GlowNetworkSavedData networkData = GlowNetworkSavedData.get(serverLevel);
            GlowWaypointSavedData waypointData = GlowWaypointSavedData.get(serverLevel);

            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new ClientboundGlowSyncPacket(
                            new ArrayList<>(networkData.getNodes()),
                            networkData.getLines(),
                            new ArrayList<>(waypointData.getAllWaypoints())
                    )
            );
        } else if (level.isClientSide()) {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new net.eeebsiekat.wanderwood.glow.client.gui.GlowWaypointScreen()
            );
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof StumpBlockEntity be) {
                be.unregisterWaypoint(serverLevel);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StumpBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return type == TheWanderwood.STUMP_BE.get()
                ? (lvl, p, st, be) -> AbstractWaypointBlockEntity.serverTick(lvl, p, st, (StumpBlockEntity) be)
                : null;
    }
}