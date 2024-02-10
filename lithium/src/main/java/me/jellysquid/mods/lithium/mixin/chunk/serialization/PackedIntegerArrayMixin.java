package me.jellysquid.mods.lithium.mixin.chunk.serialization;

import me.jellysquid.mods.lithium.common.world.chunk.CompactingPackedIntegerArray;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.world.chunk.Palette;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Extends {@link PackedIntegerArray} with a special compaction method defined in {@link CompactingPackedIntegerArray}.
 */
@Mixin(PackedIntegerArray.class)
public abstract class PackedIntegerArrayMixin implements CompactingPackedIntegerArray {
    @Shadow
    @Final
    private int size;

    @Shadow
    @Final
    private int elementBits;

    @Shadow
    @Final
    private long maxValue;

    @Shadow
    @Final
    private int elementsPerLong;

    @Shadow
    @Final
    private long[] data;

    @Override
    public <T> void compact(Palette<T> srcPalette, Palette<T> dstPalette, short[] out) {
        if (this.size >= Short.MAX_VALUE) {
            throw new IllegalStateException("Array too large");
        }

        if (this.size != out.length) {
            throw new IllegalStateException("Array size mismatch");
        }

        short[] mappings = new short[(int) (this.maxValue + 1)];

        int idx = 0;

        for (long word : this.data) {
            long bits = word;

            for (int elementIdx = 0; elementIdx < this.elementsPerLong; ++elementIdx) {
                int value = (int) (bits & this.maxValue);
                int remappedId = mappings[value];

                if (remappedId == 0) {
                    remappedId = dstPalette.index(srcPalette.get(value)) + 1;
                    mappings[value] = (short) remappedId;
                }

                out[idx] = (short) (remappedId - 1);
                bits >>= this.elementBits;

                ++idx;

                if (idx >= this.size) {
                    return;
                }
            }
        }
    }
}
