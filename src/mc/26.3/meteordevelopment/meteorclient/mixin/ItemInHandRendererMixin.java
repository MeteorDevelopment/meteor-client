/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.ArmRenderEvent;
import meteordevelopment.meteorclient.events.render.HeldItemRendererEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.HandView;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * 26.3: {@code ItemInHandRenderer} was renamed to {@link FirstPersonHandsAndItemsRenderer} and the
 * per-player hand state (items, equip heights) moved out of the renderer into
 * {@link FirstPersonHandsAndItemsRenderState}.
 *
 * <p>Porting notes:
 * <ul>
 *     <li>The swing animation is no longer read via {@code LocalPlayer.getAttackAnim(F)}; it is now
 *     the {@code AvatarRenderState.swingAnimation} field.</li>
 *     <li>The rendered items live in the render state instead of renderer fields, so the injectors
 *     capture the target method parameters.</li>
 *     <li>{@code shouldInstantlyReplaceVisibleItem} and {@code tick} were removed, so HandView's
 *     "skip swapping"/"old animations" equip-progress logic has no hook point here anymore (see
 *     {@code src/mc/26.3/README.md}).</li>
 * </ul>
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class ItemInHandRendererMixin {
    @ModifyExpressionValue(
        method = "submitHandsWithItems",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;swingAnimation:F", opcode = Opcodes.GETFIELD)
    )
    private float modifySwing(float attackValue, float partialTick, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState fpState) {
        HandView module = Modules.get().get(HandView.class);

        var swing = mc.player == null ? null : mc.player.getCurrentSwing();
        InteractionHand hand = swing != null ? swing.hand() : InteractionHand.MAIN_HAND;

        if (module.isActive()) {
            if (module.swordSlash() && hand == InteractionHand.MAIN_HAND && fpState.mainHandItem.is(ItemTags.SWORDS)) {
                return 0f;
            }
            if (hand == InteractionHand.OFF_HAND && !fpState.offHandItem.isEmpty()) {
                return attackValue + module.offSwing.get().floatValue();
            }
            if (hand == InteractionHand.MAIN_HAND && !fpState.mainHandItem.isEmpty()) {
                return attackValue + module.mainSwing.get().floatValue();
            }
        }

        return attackValue;
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V", shift = At.Shift.BEFORE))
    private void onRenderItem(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState fpState, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(HeldItemRendererEvent.get(hand, poseStack));
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IFFLnet/minecraft/world/entity/HumanoidArm;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;)V"))
    private void onRenderArm(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState fpState, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(ArmRenderEvent.get(hand, poseStack));
    }

    @Inject(method = "applyEatTransform", at = @At(value = "INVOKE", target = "Ljava/lang/Math;pow(DD)D", shift = At.Shift.BEFORE), cancellable = true)
    private void cancelTransformations(PoseStack poseStack, float frameInterp, HumanoidArm arm, float g, int useDuration, CallbackInfo ci) {
        if (Modules.get().get(HandView.class).disableFoodAnimation()) ci.cancel();
    }
}
