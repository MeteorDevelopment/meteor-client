package me.jellysquid.mods.lithium.mixin.alloc.nbt;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;

/**
 * Use {@link Object2ObjectOpenHashMap} instead of {@link HashMap} to reduce NBT memory consumption and improve
 * iteration speed.
 *
 * @author Maity
 */
@Mixin(NbtCompound.class)
public class NbtCompoundMixin {

    @Shadow
    @Final
    private Map<String, NbtElement> entries;

    @ModifyArg(
            method = "<init>()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtCompound;<init>(Ljava/util/Map;)V")
    )
    private static Map<String, NbtElement> useFasterCollection(Map<String, NbtElement> oldMap) {
        return new Object2ObjectOpenHashMap<>();
    }

    @Redirect(
            method = "<init>()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;",
                    remap = false
            )
    )
    private static HashMap<?, ?> removeOldMapAlloc() {
        return null;
    }

    /**
     * @reason Use faster collection
     * @author Maity
     */
    @Overwrite
    public NbtCompound copy() {
        // [VanillaCopy] HashMap is replaced with Object2ObjectOpenHashMap
        var map = new Object2ObjectOpenHashMap<>(Maps.transformValues(this.entries, NbtElement::copy));
        return new NbtCompound(map);
    }

    @Mixin(targets = "net.minecraft.nbt.NbtCompound$1")
    static class Type {

        @ModifyVariable(
                method = "readCompound",
                at = @At(
                        value = "INVOKE_ASSIGN",
                        target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;",
                        remap = false
                )
        )
        private static Map<String, NbtElement> useFasterCollection(Map<String, NbtElement> map) {
            return new Object2ObjectOpenHashMap<>();
        }

        @Redirect(
                method = "readCompound",
                at = @At(
                        value = "INVOKE",
                        target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;",
                        remap = false
                )
        )
        private static HashMap<?, ?> removeOldMapAlloc() {
            return null;
        }
    }
}
