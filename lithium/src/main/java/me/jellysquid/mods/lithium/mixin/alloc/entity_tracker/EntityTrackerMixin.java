package me.jellysquid.mods.lithium.mixin.alloc.entity_tracker;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.server.network.PlayerAssociatedNetworkHandler;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(ThreadedAnvilChunkStorage.EntityTracker.class)
public class EntityTrackerMixin {

    /**
     * Uses less memory, and will cache the returned iterator.
     */
    @Redirect(
            method = "<init>",
            require = 0,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Sets;newIdentityHashSet()Ljava/util/Set;",
                    remap = false
            )
    )
    private Set<PlayerAssociatedNetworkHandler> useFasterCollection() {
        return new ReferenceOpenHashSet<>();
    }
}
