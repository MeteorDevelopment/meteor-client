package minegame159.meteorclient.mixininterface;

import net.minecraft.util.math.BlockPos;

public interface IClientPlayerInteractionManager {
    public void syncSelectedSlot2();

    public double getBreakingProgress();

    public BlockPos getCurrentBreakingBlockPos();
}
