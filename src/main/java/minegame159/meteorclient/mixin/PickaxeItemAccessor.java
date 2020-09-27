package minegame159.meteorclient.mixin;

import net.minecraft.block.Block;
import net.minecraft.item.PickaxeItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(PickaxeItem.class)
public interface PickaxeItemAccessor {
    @Accessor("EFFECTIVE_BLOCKS")
    static Set<Block> getEffectiveBlocks() { return null; }
}
