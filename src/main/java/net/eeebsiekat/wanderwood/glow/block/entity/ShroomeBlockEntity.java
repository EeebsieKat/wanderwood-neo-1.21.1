package net.eeebsiekat.wanderwood.glow.block.entity;

import net.eeebsiekat.wanderwood.TheWanderwood;
import net.eeebsiekat.wanderwood.glow.data.Waypoint;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class ShroomeBlockEntity extends AbstractWaypointBlockEntity {

    public ShroomeBlockEntity(BlockPos pos, BlockState state) {
        super(TheWanderwood.SHROOME_BE.get(), pos, state);
    }

    @Override
    public double getMaxGlow() {
        return Waypoint.Type.SHROOME.getMaxGlow(); // 50,000.0
    }

    @Override
    public Waypoint.Type getWaypointType() {
        return Waypoint.Type.SHROOME;
    }

    @Override
    public boolean canGenerateGlow(ServerLevel level, BlockPos pos) {
        return true;
    }
}