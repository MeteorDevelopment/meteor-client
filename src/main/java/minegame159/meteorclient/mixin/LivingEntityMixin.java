package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.movement.HighJump;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    private HighJump highJump;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    private HighJump getHighJump() {
        if (highJump == null) highJump = ModuleManager.INSTANCE.get(HighJump.class);
        return highJump;
    }

    @Inject(method = "damage", at = @At("TAIL"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        MeteorClient.eventBus.post(EventStore.tookDamageEvent((LivingEntity) (Object) this));
    }

    @Inject(method = "getJumpVelocity", at = @At("HEAD"), cancellable = true)
    private void onGetJumpVelocity(CallbackInfoReturnable<Float> info) {
        if (getHighJump().isActive()) {
            info.setReturnValue(0.42f * highJump.getMultiplier());
        }
    }
}
