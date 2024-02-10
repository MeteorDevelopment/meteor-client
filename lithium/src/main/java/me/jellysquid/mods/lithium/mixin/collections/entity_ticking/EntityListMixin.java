package me.jellysquid.mods.lithium.mixin.collections.entity_ticking;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.Entity;
import net.minecraft.world.EntityList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityList.class)
public class EntityListMixin {
    @Shadow
    private @Nullable Int2ObjectMap<Entity> iterating;

    @Shadow
    private Int2ObjectMap<Entity> entities;

    @Shadow
    private Int2ObjectMap<Entity> temp;

    /**
     * @author 2No2Name
     * @reason avoid slow iteration, allocate instead
     */
    @Overwrite
    private void ensureSafe() {
        if (this.iterating == this.entities) {
            this.temp = this.entities;
            this.entities = ((Int2ObjectLinkedOpenHashMap<Entity>) this.entities).clone();
        }
    }
}
