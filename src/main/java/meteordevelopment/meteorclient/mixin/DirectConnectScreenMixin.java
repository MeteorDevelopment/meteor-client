/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.ServerSpoof;
import net.minecraft.client.gui.screen.DirectConnectScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.UnknownHostException;

@Mixin(DirectConnectScreen.class)
public class DirectConnectScreenMixin {
    @Shadow
    private TextFieldWidget addressField;

    @Inject(method = "saveAndClose", at = @At("HEAD"))
    private void onSaveAndClose(CallbackInfo ci) {
        if (!Modules.get().get(ServerSpoof.class).bypassAntiDirectConnect.get()) return;
        MultiplayerServerListPinger slp = new MultiplayerServerListPinger();
        try {
            slp.add(new ServerInfo("", this.addressField.getText(), ServerInfo.ServerType.OTHER), () -> {});
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }
        try {
            Thread.sleep(Modules.get().get(ServerSpoof.class).bypassAntiDirectConnectDelay.get());
        } catch (InterruptedException ignored) {}
    }
}
