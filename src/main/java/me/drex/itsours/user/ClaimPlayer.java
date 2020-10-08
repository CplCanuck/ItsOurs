package me.drex.itsours.user;

import me.drex.itsours.claim.AbstractClaim;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;

public interface ClaimPlayer {

    void setLastShow(AbstractClaim claim, BlockPos pos, ServerWorld world);

    AbstractClaim getLastShowClaim();

    BlockPos getLastShowPos();

    ServerWorld getLastShowWorld();

    boolean arePositionsSet();

    void setLeftPosition(BlockPos pos);

    void setRightPosition(BlockPos pos);

    BlockPos getRightPosition();

    BlockPos getLeftPosition();

    void sendError(Text error);

    void sendError(String error);

    void sendMessage(Text text);

    void toggleFlight();

    boolean flightEnabled();

    void cacheFlight(boolean value);

    boolean getFlightCache();


}
