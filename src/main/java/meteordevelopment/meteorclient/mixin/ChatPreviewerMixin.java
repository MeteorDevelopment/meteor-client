/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.network.ChatPreviewer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatPreviewer.class)
public class ChatPreviewerMixin {
    @Inject(at=@At("HEAD"),method = "tryRequest",cancellable = true)
    public void noRequest(String message, CallbackInfo ci) {
        ci.cancel();
    }
    @Inject(at=@At("HEAD"),method = "onResponse",cancellable = true)
    public void noModify(int id, Text text, CallbackInfo ci) {
        ci.cancel();
    }
}