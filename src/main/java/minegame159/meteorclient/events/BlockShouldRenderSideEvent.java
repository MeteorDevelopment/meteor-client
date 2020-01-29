package minegame159.meteorclient.events;

import net.minecraft.block.BlockState;

public class BlockShouldRenderSideEvent extends Cancellable {
    public BlockState state;
    public boolean shouldRenderSide;
}
