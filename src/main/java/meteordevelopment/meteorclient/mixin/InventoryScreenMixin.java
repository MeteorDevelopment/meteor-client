/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @ModifyVariable(method = "drawEntity(IIIFFLnet/minecraft/entity/LivingEntity;)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static LivingEntity modifyEntity(LivingEntity entity) {
        InventoryTweaks inventoryTweaks = Modules.get().get(InventoryTweaks.class);
        if (inventoryTweaks.isActive()) {
            if (inventoryTweaks.inventoryModelEntity.get() == EntityType.PLAYER) return entity;
            return (LivingEntity) inventoryTweaks.inventoryModelEntity.get().create(mc.world);
        }
        return entity;
    }

    @ModifyVariable(method = "drawEntity(IIIFFLnet/minecraft/entity/LivingEntity;)V", at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private static int modifySize(int size) {
        InventoryTweaks inventoryTweaks = Modules.get().get(InventoryTweaks.class);
        if (inventoryTweaks.isActive()) {
            return inventoryTweaks.inventoryModelScale.get();
        }
        return size;
    }

    @ModifyVariable(method = "drawEntity(IIIFFLnet/minecraft/entity/LivingEntity;)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static float modifyMouseX(float mouseX) {
        InventoryTweaks inventoryTweaks = Modules.get().get(InventoryTweaks.class);
        if (inventoryTweaks.isActive() && !inventoryTweaks.followMouse.get()) {
            return 0.0f;
        }
        return mouseX;
    }

    @ModifyVariable(method = "drawEntity(IIIFFLnet/minecraft/entity/LivingEntity;)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private static float modifyMouseY(float mouseY) {
        InventoryTweaks inventoryTweaks = Modules.get().get(InventoryTweaks.class);
        if (inventoryTweaks.isActive() && !inventoryTweaks.followMouse.get()) {
            return 0.0f;
        }
        return mouseY;
    }

    @Inject(method = "drawEntity", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;runAsFancy(Ljava/lang/Runnable;)V"))
    private static void onDrawEntity(
        int x, int y, int size, float mouseX, float mouseY, LivingEntity entity, CallbackInfo ci) {
        InventoryTweaks inventoryTweaks = Modules.get().get(InventoryTweaks.class);
        if (inventoryTweaks.isActive() && !inventoryTweaks.followMouse.get()) {
            entity.setPitch(inventoryTweaks.headPitch.get().floatValue());

            entity.headYaw = inventoryTweaks.headYaw.get().floatValue();
            entity.bodyYaw = inventoryTweaks.bodyYaw.get().floatValue();
            entity.prevHeadYaw = inventoryTweaks.headYaw.get().floatValue();
        }
    }
}
