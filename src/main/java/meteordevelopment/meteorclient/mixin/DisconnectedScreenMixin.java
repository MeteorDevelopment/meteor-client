/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Shadow
    @Final
    private LinearLayout layout;
    @Unique
    private Button reconnectBtn;
    @Unique
    private double time = Modules.get().get(AutoReconnect.class).time.get() * 20;

    protected DisconnectedScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/LinearLayout;arrangeElements()V", shift = At.Shift.BEFORE))
    private void addButtons(CallbackInfo ci) {
        AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);

        if (autoReconnect.lastServerConnection != null && !autoReconnect.button.get()) {
            reconnectBtn = new Button.Builder(Component.literal(getText()), _ -> tryConnecting()).build();
            layout.addChild(reconnectBtn);

            layout.addChild(
                new Button.Builder(Component.literal("Toggle Auto Reconnect"), _ -> {
                    autoReconnect.toggle();
                    reconnectBtn.setMessage(Component.literal(getText()));
                    time = autoReconnect.time.get() * 20;
                }).build()
            );
        }
    }

    @Override
    public void tick() {
        AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);
        if (!autoReconnect.isActive() || autoReconnect.lastServerConnection == null) return;

        if (time <= 0) {
            tryConnecting();
        } else {
            time -= 1;
            if (reconnectBtn != null) reconnectBtn.setMessage(Component.literal(getText()));
        }
    }

    @Unique
    private String getText() {
        String reconnectText = "Reconnect";
        if (Modules.get().isActive(AutoReconnect.class)) reconnectText += " " + String.format("(%.1f)", time / 20);
        return reconnectText;
    }

    @Unique
    private void tryConnecting() {
        var lastServer = Modules.get().get(AutoReconnect.class).lastServerConnection;
        ConnectScreen.startConnecting(new TitleScreen(), mc, lastServer.left(), lastServer.right(), false, null);
    }
}
