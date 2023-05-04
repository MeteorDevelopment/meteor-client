/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.toast.*;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Deque;

@Mixin(ToastManager.class)
public class ToastManagerMixin {
    @Shadow
    @Final
    private Deque<Toast> toastQueue;

    @Inject(method = "draw", at = @At("HEAD"))
    private void draw(MatrixStack matrices, CallbackInfo ci) {
        if(this.toastQueue.isEmpty()) return;
        if(!Modules.get().get(NoRender.class).isActive() || !Modules.get().get(NoRender.class).noToasts()) return;

        Toast currentToast = toastQueue.getFirst();
        if(currentToast instanceof AdvancementToast && Modules.get().get(NoRender.class).noAdvancementToasts()) toastQueue.removeFirst();
        if(currentToast instanceof RecipeToast && Modules.get().get(NoRender.class).noRecipeToasts()) toastQueue.removeFirst();
        if(currentToast instanceof SystemToast && Modules.get().get(NoRender.class).noSystemToasts()) toastQueue.removeFirst();
        if(currentToast instanceof TutorialToast && Modules.get().get(NoRender.class).noTutorialToasts()) toastQueue.removeFirst();
    }
}
