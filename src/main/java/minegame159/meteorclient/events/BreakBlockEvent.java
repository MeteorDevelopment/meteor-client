package minegame159.meteorclient.events;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BreakBlockEvent {
    public BlockPos blockPos;

    public BlockState getBlockState(World world) {
        return world.getBlockState(blockPos);
    }
}
