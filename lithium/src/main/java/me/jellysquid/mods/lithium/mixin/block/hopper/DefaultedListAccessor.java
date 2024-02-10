package me.jellysquid.mods.lithium.mixin.block.hopper;

import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(DefaultedList.class)
public interface DefaultedListAccessor<T> {
    @Accessor("delegate")
    List<T> getDelegate();
}
