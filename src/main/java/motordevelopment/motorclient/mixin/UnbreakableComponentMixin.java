/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.render.BetterTooltips;
import net.minecraft.component.type.UnbreakableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(UnbreakableComponent.class)
public abstract class UnbreakableComponentMixin {
    @ModifyExpressionValue(method = "appendTooltip", at = @At(value = "FIELD", target = "Lnet/minecraft/component/type/UnbreakableComponent;showInTooltip:Z"))
    private boolean modifyShowInTooltip(boolean original) {
        BetterTooltips bt = Modules.get().get(BetterTooltips.class);
        return (bt.isActive() && bt.unbreakable.get()) || original;
    }
}
