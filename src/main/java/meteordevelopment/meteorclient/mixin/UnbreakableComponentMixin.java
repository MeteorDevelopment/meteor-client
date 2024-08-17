/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
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
