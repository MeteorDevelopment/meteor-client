package me.jellysquid.mods.lithium.mixin.experimental.entity.item_entity_merging;

import me.jellysquid.mods.lithium.common.entity.item.ItemEntityLazyIterationConsumer;
import me.jellysquid.mods.lithium.common.world.ItemEntityHelper;
import me.jellysquid.mods.lithium.common.world.WorldHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.entity.SectionedEntityCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {
    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract ItemStack getStack();

    @Redirect(
            method = "tryMerge()V",
            at = @At(
                    value = "INVOKE", target = "Lnet/minecraft/world/World;getEntitiesByClass(Ljava/lang/Class;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Ljava/util/List;"
            )
    )
    private List<ItemEntity> getItems(World world, Class<ItemEntity> itemEntityClass, Box box, Predicate<ItemEntity> predicate) {
        SectionedEntityCache<Entity> cache = WorldHelper.getEntityCacheOrNull(world);
        if (cache != null) {
            ItemEntityLazyIterationConsumer itemEntityConsumer = new ItemEntityLazyIterationConsumer((ItemEntity) (Object) this, box, predicate);
            ItemEntityHelper.consumeItemEntitiesForMerge(cache, (ItemEntity) (Object) this, box, itemEntityConsumer);
            return itemEntityConsumer.getMergeEntities();
        }

        return world.getEntitiesByClass(itemEntityClass, box, predicate);
    }
}
