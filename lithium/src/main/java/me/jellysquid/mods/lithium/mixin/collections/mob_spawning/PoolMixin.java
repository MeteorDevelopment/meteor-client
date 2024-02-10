package me.jellysquid.mods.lithium.mixin.collections.mob_spawning;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.lithium.common.util.collections.HashedReferenceList;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.collection.Weighted;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

@Mixin(Pool.class)
public class PoolMixin<E extends Weighted> {

    @Mutable
    @Shadow
    @Final
    private ImmutableList<E> entries;
    //Need a separate variable due to entries being type ImmutableList
    private List<E> entryHashList;

    @Inject(method = "<init>(Ljava/util/List;)V", at = @At("RETURN"))
    private void init(List<? extends E> entries, CallbackInfo ci) {
        //We are using reference equality here, because all vanilla implementations of Weighted use reference equality
        this.entryHashList = this.entries.size() > 4 ? this.entries : Collections.unmodifiableList(new HashedReferenceList<>(this.entries));
    }

    /**
     * @author 2No2Name
     * @reason return a collection with a faster contains() call
     */
    @Overwrite
    public List<E> getEntries() {
        return this.entryHashList;
    }
}
