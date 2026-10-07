/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import meteordevelopment.meteorclient.utils.tooltip.MeteorTooltipData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(value = GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
    // Both overloads that take an Optional<TooltipComponent>: the List<Component> one is the item path
    // (setTooltipForNextFrame(Font, ItemStack, II)), the List<FormattedCharSequence> one everything else.
    @Inject(
        method = {
            "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;Z)V",
            "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;IIZLnet/minecraft/resources/Identifier;)V"
        },
        at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V", shift = At.Shift.BEFORE)
    )
    private void onDrawTooltip(CallbackInfo ci, @Local(ordinal = 0) Optional<TooltipComponent> data, @Local(name = "components") List<ClientTooltipComponent> components) {
        if (data.isPresent() && data.get() instanceof MeteorTooltipData meteorTooltipData)
            components.add(meteorTooltipData.getComponent());
    }

    @ModifyReceiver(
        method = {
            "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;Z)V",
            "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;IIZLnet/minecraft/resources/Identifier;)V"
        },
        at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V")
    )
    private Optional<TooltipComponent> onDrawTooltip_modifyIfPresentReceiver(Optional<TooltipComponent> data, Consumer<TooltipComponent> action) {
        if (data.isPresent() && data.get() instanceof MeteorTooltipData) return Optional.empty();
        return data;
    }
}
