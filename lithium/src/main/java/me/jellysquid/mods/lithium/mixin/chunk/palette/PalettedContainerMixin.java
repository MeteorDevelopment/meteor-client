package me.jellysquid.mods.lithium.mixin.chunk.palette;

import me.jellysquid.mods.lithium.common.world.chunk.LithiumHashPalette;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.*;

import static net.minecraft.world.chunk.PalettedContainer.PaletteProvider.ARRAY;
import static net.minecraft.world.chunk.PalettedContainer.PaletteProvider.SINGULAR;

@Mixin(PalettedContainer.PaletteProvider.class)
public abstract class PalettedContainerMixin {
    @Mutable
    @Shadow
    @Final
    public static PalettedContainer.PaletteProvider BLOCK_STATE;

    @Unique
    private static final PalettedContainer.DataProvider<?>[] BLOCKSTATE_DATA_PROVIDERS;
    @Unique
    private static final PalettedContainer.DataProvider<?>[] BIOME_DATA_PROVIDERS;


    @Unique
    private static final Palette.Factory HASH = LithiumHashPalette::create;
    @Mutable
    @Shadow
    @Final
    public static PalettedContainer.PaletteProvider BIOME;
    @Shadow
    @Final
    static Palette.Factory ID_LIST;

    /*
     * @reason Replace the hash palette from vanilla with our own and change the threshold for usage to only 3 bits,
     * as our implementation performs better at smaller key ranges.
     * @author JellySquid, 2No2Name (avoid DataProvider duplication, use hash palette for 3 bit biomes)
     */
    static {
        Palette.Factory idListFactory = ID_LIST;

        PalettedContainer.DataProvider<?> arrayDataProvider4bit = new PalettedContainer.DataProvider<>(ARRAY, 4);
        PalettedContainer.DataProvider<?> hashDataProvider4bit = new PalettedContainer.DataProvider<>(HASH, 4);
        BLOCKSTATE_DATA_PROVIDERS = new PalettedContainer.DataProvider<?>[]{
                new PalettedContainer.DataProvider<>(SINGULAR, 0),
                // Bits 1-4 must all pass 4 bits as parameter, otherwise chunk sections will corrupt.
                arrayDataProvider4bit,
                arrayDataProvider4bit,
                hashDataProvider4bit,
                hashDataProvider4bit,
                new PalettedContainer.DataProvider<>(HASH, 5),
                new PalettedContainer.DataProvider<>(HASH, 6),
                new PalettedContainer.DataProvider<>(HASH, 7),
                new PalettedContainer.DataProvider<>(HASH, 8)
        };

        BLOCK_STATE = new PalettedContainer.PaletteProvider(4) {
            @Override
            public <A> PalettedContainer.DataProvider<A> createDataProvider(IndexedIterable<A> idList, int bits) {
                if (bits >= 0 && bits < BLOCKSTATE_DATA_PROVIDERS.length) {
                    //noinspection unchecked
                    return (PalettedContainer.DataProvider<A>) BLOCKSTATE_DATA_PROVIDERS[bits];
                }
                return new PalettedContainer.DataProvider<>(idListFactory, MathHelper.ceilLog2(idList.size()));
            }
        };

        BIOME_DATA_PROVIDERS = new PalettedContainer.DataProvider<?>[]{
                new PalettedContainer.DataProvider<>(SINGULAR, 0),
                new PalettedContainer.DataProvider<>(ARRAY, 1),
                new PalettedContainer.DataProvider<>(ARRAY, 2),
                new PalettedContainer.DataProvider<>(HASH, 3)
        };


        BIOME = new PalettedContainer.PaletteProvider(2) {
            @Override
            public <A> PalettedContainer.DataProvider<A> createDataProvider(IndexedIterable<A> idList, int bits) {
                if (bits >= 0 && bits < BIOME_DATA_PROVIDERS.length) {
                    //noinspection unchecked
                    return (PalettedContainer.DataProvider<A>) BIOME_DATA_PROVIDERS[bits];
                }
                return new PalettedContainer.DataProvider<>(idListFactory, MathHelper.ceilLog2(idList.size()));
            }
        };
    }
}
