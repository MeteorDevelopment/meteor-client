/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.render.BetterTooltips;
import net.minecraft.component.type.JukeboxPlayableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(JukeboxPlayableComponent.class)
public class JukeboxPlayableComponentMixin {
    @ModifyExpressionValue(method = "appendTooltip(Lnet/minecraft/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/item/tooltip/TooltipType;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/component/type/JukeboxPlayableComponent;showInTooltip:Z"))
    private boolean modifyShowInTooltip(boolean original) {
        BetterTooltips bt = Modules.get().get(BetterTooltips.class);
        return (bt.isActive() && bt.jukeboxPlayable.get()) || original;
    }
}
