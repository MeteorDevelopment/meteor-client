package me.jellysquid.mods.lithium.mixin.ai.poi;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.lithium.common.world.interests.PointOfInterestSetExtended;
import me.jellysquid.mods.lithium.common.world.interests.iterator.SinglePointOfInterestTypeFilter;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestSet;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(PointOfInterestSet.class)
public class PointOfInterestSetMixin implements PointOfInterestSetExtended {
    @Mutable
    @Shadow
    @Final
    private Map<RegistryEntry<PointOfInterestType>, Set<PointOfInterest>> pointsOfInterestByType;

    private static <K, V> Iterable<? extends Map.Entry<K, V>> getPointsByTypeIterator(Map<K, V> map) {
        if (map instanceof Reference2ReferenceMap) {
            return Reference2ReferenceMaps.fastIterable((Reference2ReferenceMap<K, V>) map);
        } else {
            return map.entrySet();
        }
    }

    @Inject(method = "<init>(Ljava/lang/Runnable;ZLjava/util/List;)V", at = @At("RETURN"))
    private void reinit(Runnable updateListener, boolean bl, List<PointOfInterest> list, CallbackInfo ci) {
        this.pointsOfInterestByType = new Reference2ReferenceOpenHashMap<>(this.pointsOfInterestByType);
    }

    @Override
    public void collectMatchingPoints(Predicate<RegistryEntry<PointOfInterestType>> type, PointOfInterestStorage.OccupationStatus status, Consumer<PointOfInterest> consumer) {
        if (type instanceof SinglePointOfInterestTypeFilter) {
            this.getWithSingleTypeFilter(((SinglePointOfInterestTypeFilter) type).getType(), status, consumer);
        } else {
            this.getWithDynamicTypeFilter(type, status, consumer);
        }
    }

    private void getWithDynamicTypeFilter(Predicate<RegistryEntry<PointOfInterestType>> type, PointOfInterestStorage.OccupationStatus status, Consumer<PointOfInterest> consumer) {
        for (Map.Entry<RegistryEntry<PointOfInterestType>, Set<PointOfInterest>> entry : getPointsByTypeIterator(this.pointsOfInterestByType)) {
            if (!type.test(entry.getKey())) {
                continue;
            }

            if (!entry.getValue().isEmpty()) {
                for (PointOfInterest poi : entry.getValue()) {
                    if (status.getPredicate().test(poi)) {
                        consumer.accept(poi);
                    }
                }
            }
        }
    }

    private void getWithSingleTypeFilter(RegistryEntry<PointOfInterestType> type, PointOfInterestStorage.OccupationStatus status, Consumer<PointOfInterest> consumer) {
        Set<PointOfInterest> entries = this.pointsOfInterestByType.get(type);

        if (entries == null || entries.isEmpty()) {
            return;
        }

        for (PointOfInterest poi : entries) {
            if (status.getPredicate().test(poi)) {
                consumer.accept(poi);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Redirect(method = "add(Lnet/minecraft/world/poi/PointOfInterest;)Z",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"))
    private <K, V> K computeIfAbsent(Map<K, V> map, K key, Function<? super K, ? extends V> mappingFunction) {
        return (K) map.computeIfAbsent(key, o -> (V) new ObjectOpenHashSet<>());
    }
}
