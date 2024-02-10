package me.jellysquid.mods.lithium.mixin.util.world_border_listener;

import me.jellysquid.mods.lithium.common.world.listeners.WorldBorderListenerOnce;
import me.jellysquid.mods.lithium.common.world.listeners.WorldBorderListenerOnceMulti;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldBorder.class)
public abstract class WorldBorderMixin {
    @Shadow
    private WorldBorder.Area area;

    @Shadow
    public abstract void addListener(WorldBorderListener listener);

    private final WorldBorderListenerOnceMulti worldBorderListenerOnceMulti = new WorldBorderListenerOnceMulti();

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void registerSimpleWorldBorderListenerMulti(CallbackInfo ci) {
        this.addListener(this.worldBorderListenerOnceMulti);
    }


    @Inject(
            method = "addListener",
            at = @At("HEAD"),
            cancellable = true
    )
    private void addSimpleListenerOnce(WorldBorderListener listener, CallbackInfo ci) {
        if (listener instanceof WorldBorderListenerOnce simpleListener) {
            ci.cancel();
            this.worldBorderListenerOnceMulti.add(simpleListener);
        }
    }

    /**
     * @author 2No2Name
     * @reason notify listeners on change
     */
    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder$Area;getAreaInstance()Lnet/minecraft/world/border/WorldBorder$Area;")
    )
    public WorldBorder.Area getUpdatedArea(WorldBorder.Area instance) {
        WorldBorder.Area areaInstance = this.area.getAreaInstance();
        if (areaInstance != this.area) {
            this.area = areaInstance;
            this.worldBorderListenerOnceMulti.onAreaReplaced((WorldBorder) (Object) this);
        }
        return areaInstance;
    }
}
