package me.jellysquid.mods.lithium.mixin.alloc.chunk_ticking;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;

@Mixin(ServerChunkManager.class)
public class ServerChunkManagerMixin {
    private final ArrayList<ChunkHolder> cachedChunkList = new ArrayList<>();

    @Redirect(
            method = "tickChunks()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Lists;newArrayListWithCapacity(I)Ljava/util/ArrayList;",
                    remap = false
            )
    )
    private ArrayList<ChunkHolder> redirectChunksListClone(int initialArraySize) {
        ArrayList<ChunkHolder> list = this.cachedChunkList;
        list.clear(); // Ensure the list is empty before re-using it
        list.ensureCapacity(initialArraySize);

        return list;
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;Z)V", at = @At("HEAD"))
    private void preTick(BooleanSupplier shouldKeepTicking, boolean tickChunks, CallbackInfo ci) {
        // Ensure references aren't leaked through this list
        this.cachedChunkList.clear();
    }
}
