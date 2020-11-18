package minegame159.meteorclient.mixininterface;

import net.minecraft.util.math.BlockPos;

public interface IClientPlayerInteractionManager {
    void syncSelectedSlot2();

    double getBreakingProgress();

    BlockPos getCurrentBreakingBlockPos();
}
