/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TooltipDisplay.class)
public abstract class TooltipDisplayMixin {
    @ModifyExpressionValue(method = "shows", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/component/TooltipDisplay;hideTooltip:Z"))
    private boolean modifyHideTooltip(boolean original) {
        return original && !Modules.get().get(BetterTooltips.class).tooltip.get();
    }

    @ModifyExpressionValue(method = "shows", at = @At(value = "INVOKE", target = "Ljava/util/SequencedSet;contains(Ljava/lang/Object;)Z"))
    private boolean modifyHiddenComponents(boolean original) {
        return original && !Modules.get().get(BetterTooltips.class).additional.get();
    }
}
