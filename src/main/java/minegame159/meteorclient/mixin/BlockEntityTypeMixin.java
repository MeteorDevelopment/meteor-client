package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IBlockEntityType;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(BlockEntityType.class)
public class BlockEntityTypeMixin implements IBlockEntityType {
    @Shadow @Final private Set<Block> blocks;

    @Override
    public Set<Block> getBlocks() {
        return blocks;
    }
}
