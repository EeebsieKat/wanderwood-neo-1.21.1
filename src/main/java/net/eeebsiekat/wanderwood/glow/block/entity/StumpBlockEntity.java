package net.eeebsiekat.wanderwood.glow.block.entity;

import net.eeebsiekat.wanderwood.TheWanderwood;
import net.eeebsiekat.wanderwood.glow.data.Waypoint;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class StumpBlockEntity extends AbstractWaypointBlockEntity {

    public StumpBlockEntity(BlockPos pos, BlockState state) {
        super(TheWanderwood.STUMP_BE.get(), pos, state);
    }

    @Override
    public double getMaxGlow() {
        return Waypoint.Type.STUMP.getMaxGlow(); // 150,000.0
    }

    @Override
    public Waypoint.Type getWaypointType() {
        return Waypoint.Type.STUMP;
    }

    @Override
    public boolean canGenerateGlow(ServerLevel level, BlockPos pos) {
        return level.canSeeSky(pos.above());
    }
}