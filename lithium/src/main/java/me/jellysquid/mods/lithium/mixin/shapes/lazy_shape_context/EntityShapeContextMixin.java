package me.jellysquid.mods.lithium.mixin.shapes.lazy_shape_context;

import net.minecraft.block.EntityShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(EntityShapeContext.class)
public class EntityShapeContextMixin {
    @Mutable
    @Shadow
    @Final
    private ItemStack heldItem;

    @Mutable
    @Shadow
    @Final
    private Predicate<FluidState> walkOnFluidPredicate;

    @Shadow
    @Final
    @Nullable
    private Entity entity;

    /**
     * Mixin the instanceof to always return false to avoid the expensive inventory access.
     * No need to use Opcodes.INSTANCEOF or similar.
     */
    @ModifyConstant(
            method = "<init>(Lnet/minecraft/entity/Entity;)V",
            constant = @Constant(classValue = LivingEntity.class, ordinal = 0)
    )
    private static boolean redirectInstanceOf(Object obj, Class<?> clazz) {
        return false;
    }

    @ModifyConstant(
            method = "<init>(Lnet/minecraft/entity/Entity;)V",
            constant = @Constant(classValue = LivingEntity.class, ordinal = 2)
    )
    private static boolean redirectInstanceOf2(Object obj, Class<?> clazz) {
        return false;
    }

    @Inject(
            method = "<init>(Lnet/minecraft/entity/Entity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/EntityShapeContext;<init>(ZDLnet/minecraft/item/ItemStack;Ljava/util/function/Predicate;Lnet/minecraft/entity/Entity;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void initFields(Entity entity, CallbackInfo ci) {
        this.heldItem = null;
        this.walkOnFluidPredicate = null;
    }

    @Inject(
            method = "isHolding(Lnet/minecraft/item/Item;)Z",
            at = @At("HEAD")
    )
    public void isHolding(Item item, CallbackInfoReturnable<Boolean> cir) {
        if (this.heldItem == null) {
            this.heldItem = this.entity instanceof LivingEntity ? ((LivingEntity) this.entity).getMainHandStack() : ItemStack.EMPTY;
        }
    }

    @Inject(
            method = "canWalkOnFluid(Lnet/minecraft/fluid/FluidState;Lnet/minecraft/fluid/FluidState;)Z",
            at = @At("HEAD")
    )
    public void canWalkOnFluid(FluidState state, FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        if (this.walkOnFluidPredicate == null) {
            if (this.entity instanceof LivingEntity livingEntity) {
                this.walkOnFluidPredicate = livingEntity::canWalkOnFluid;
            } else {
                this.walkOnFluidPredicate = (liquid) -> false;
            }
        }
    }
}
