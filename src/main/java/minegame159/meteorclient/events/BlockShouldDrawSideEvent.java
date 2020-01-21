package minegame159.meteorclient.events;

import minegame159.jes.Event;
import net.minecraft.block.BlockState;

public class BlockShouldDrawSideEvent extends Event {
    public BlockState state;
    public boolean shouldRenderSide;

    @Override
    public boolean isCancellable() {
        return true;
    }
}
