package me.jellysquid.mods.lithium.mixin.experimental.spawning;

import com.google.common.collect.AbstractIterator;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import me.jellysquid.mods.lithium.common.world.ChunkAwareEntityIterable;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.SectionedEntityCache;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;

@Mixin(SectionedEntityCache.class)
public abstract class SectionedEntityCacheMixin<T extends EntityLike> implements ChunkAwareEntityIterable<T> {

    @Shadow
    @Final
    private Long2ObjectMap<EntityTrackingSection<T>> trackingSections;

    @Override
    public Iterable<T> lithiumIterateEntitiesInTrackedSections() {
        ObjectCollection<EntityTrackingSection<T>> sections = this.trackingSections.values();
        return () -> {
            ObjectIterator<EntityTrackingSection<T>> sectionsIterator = sections.iterator();
            return new AbstractIterator<T>() {
                Iterator<T> entityIterator;
                @Nullable
                @Override
                protected T computeNext() {
                    if (this.entityIterator != null && this.entityIterator.hasNext()) {
                        return this.entityIterator.next();
                    }
                    while (sectionsIterator.hasNext()) {
                        EntityTrackingSection<T> section = sectionsIterator.next();
                        if (section.getStatus().shouldTrack() && !section.isEmpty()) {
                            //noinspection unchecked
                            this.entityIterator = ((EntityTrackingSectionAccessor<T>) section).getCollection().iterator();
                            if (this.entityIterator.hasNext()) {
                                return this.entityIterator.next();
                            }
                        }
                    }
                    return this.endOfData();
                }
            };
        };
    }

}
