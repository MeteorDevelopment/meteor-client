package me.jellysquid.mods.lithium.mixin.collections.mob_spawning;

import com.google.common.collect.Maps;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.collection.Pool;
import net.minecraft.world.biome.SpawnSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(SpawnSettings.class)
public class SpawnSettingsMixin {
    @Mutable
    @Shadow
    @Final
    private Map<SpawnGroup, Pool<SpawnSettings.SpawnEntry>> spawners;

    /**
     * Re-initialize the spawn category lists with a much faster backing collection type for enum keys. This provides
     * a modest speed-up for mob spawning as {@link SpawnSettings#getSpawnEntries(SpawnGroup)} is a rather hot method.
     */
    @Inject(method = "<init>(FLjava/util/Map;Ljava/util/Map;)V", at = @At("RETURN"))
    private void reinit(float creatureSpawnProbability, Map<SpawnGroup, Pool<SpawnSettings.SpawnEntry>> spawners, Map<EntityType<?>, SpawnSettings.SpawnDensity> spawnCosts, CallbackInfo ci) {
        Map<SpawnGroup, Pool<SpawnSettings.SpawnEntry>> spawns = Maps.newEnumMap(SpawnGroup.class);

        for (Map.Entry<SpawnGroup, Pool<SpawnSettings.SpawnEntry>> entry : this.spawners.entrySet()) {
            spawns.put(entry.getKey(), entry.getValue());
        }

        this.spawners = spawns;
    }
}
