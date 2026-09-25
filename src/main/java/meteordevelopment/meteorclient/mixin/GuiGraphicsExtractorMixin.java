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

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(value = GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
    // Both overloads convert the tooltip image with ClientTooltipComponent.create(), which returns null for MeteorTooltipData,
    // so add Meteor's own component instead and skip the vanilla conversion
    @ModifyReceiver(method = {
        "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;IIZLnet/minecraft/resources/Identifier;)V",
        "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;Z)V"
    }, at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"))
    private Optional<TooltipComponent> onDrawTooltip(Optional<TooltipComponent> data, Consumer<TooltipComponent> action, @Local(name = "components") List<ClientTooltipComponent> components) {
        if (data.isPresent() && data.get() instanceof MeteorTooltipData meteorTooltipData) {
            components.add(meteorTooltipData.getComponent());
            return Optional.empty();
        }

        return data;
    }
}
