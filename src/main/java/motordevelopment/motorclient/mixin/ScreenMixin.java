/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.commands.Commands;
import motordevelopment.motorclient.systems.config.Config;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.movement.GUIMove;
import motordevelopment.motorclient.systems.modules.render.NoRender;
import motordevelopment.motorclient.utils.Utils;
import motordevelopment.motorclient.utils.misc.text.MotorClickEvent;
import motordevelopment.motorclient.utils.misc.text.RunnableClickEvent;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static net.minecraft.client.util.InputUtil.*;

@Mixin(value = Screen.class, priority = 500) // needs to be before baritone
public abstract class ScreenMixin {
    @Inject(method = "renderInGameBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderInGameBackground(CallbackInfo info) {
        if (Utils.canUpdate() && Modules.get().get(NoRender.class).noGuiBackground())
            info.cancel();
    }

    @Inject(method = "handleTextClick", at = @At(value = "HEAD"), cancellable = true)
    private void onInvalidClickEvent(@Nullable Style style, CallbackInfoReturnable<Boolean> cir) {
        if (style == null || !(style.getClickEvent() instanceof RunnableClickEvent runnableClickEvent)) return;

        runnableClickEvent.runnable.run();
        cir.setReturnValue(true);
    }

    @Inject(method = "handleTextClick", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V", ordinal = 1, remap = false), cancellable = true)
    private void onRunCommand(Style style, CallbackInfoReturnable<Boolean> cir) {
        if (style.getClickEvent() instanceof MotorClickEvent clickEvent && clickEvent.getValue().startsWith(Config.get().prefix.get())) {
            try {
                Commands.dispatch(style.getClickEvent().getValue().substring(Config.get().prefix.get().length()));
                cir.setReturnValue(true);
            } catch (CommandSyntaxException e) {
                MotorClient.LOG.error("Failed to run command", e);
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) (this) instanceof ChatScreen) return;
        GUIMove guiMove = Modules.get().get(GUIMove.class);
        List<Integer> arrows = List.of(GLFW_KEY_RIGHT, GLFW_KEY_LEFT, GLFW_KEY_DOWN,  GLFW_KEY_UP);
        if ((guiMove.disableArrows() && arrows.contains(keyCode)) || (guiMove.disableSpace() && keyCode == GLFW_KEY_SPACE)) {
            cir.setReturnValue(true);
        }
    }
}
