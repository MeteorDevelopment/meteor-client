package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IBlockHitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockHitResult.class)
public class BlockHitResultMixin implements IBlockHitResult {
    @Shadow private Direction side;

    @Override
    public void setSide(Direction direction) {
        this.side = direction;
    }
}
