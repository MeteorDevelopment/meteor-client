/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import meteordevelopment.meteorclient.utils.tooltip.MeteorTooltipData;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(value = DrawContext.class)
public abstract class DrawContextMixin {
    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/util/Identifier;)V", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V", shift = At.Shift.BEFORE))
    private void onDrawTooltip(TextRenderer textRenderer, List<Text> text, Optional<TooltipData> data, int x, int y, @Nullable Identifier texture, CallbackInfo info, @Local(ordinal = 1) List<TooltipComponent> list) {
        if (data.isPresent() && data.get() instanceof MeteorTooltipData meteorTooltipData)
            list.add(meteorTooltipData.getComponent());
    }

    @ModifyReceiver(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/util/Identifier;)V", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"))
    private Optional<TooltipData> onDrawTooltip_modifyIfPresentReceiver(Optional<TooltipData> data, Consumer<TooltipData> consumer) {
        if (data.isPresent() && data.get() instanceof MeteorTooltipData) return Optional.empty();
        return data;
    }
}
