package me.jellysquid.mods.lithium.mixin.world.temperature_cache;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Biome.class)
public abstract class BiomeMixin {

    @Shadow
    protected abstract float computeTemperature(BlockPos pos);

    /**
     * @author 2No2Name
     * @reason Remove caching
     */
    @Deprecated
    @Overwrite
    public float getTemperature(BlockPos blockPos) {
        return this.computeTemperature(blockPos);
    }
}
