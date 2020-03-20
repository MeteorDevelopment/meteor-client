package minegame159.meteorclient.events;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class StartBreakingBlockEvent extends Cancellable {
    public BlockPos blockPos;
    public Direction direction;
}
