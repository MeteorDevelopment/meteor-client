/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Shadow private int reasonHeight;
    @Unique private ButtonWidget reconnectBtn;
    @Unique private final AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);
    @Unique private double time = autoReconnect.time.get() * 20;

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onRenderBackground(CallbackInfo info) {
        if (autoReconnect.isActive() && autoReconnect.lastServerAddr != null) {
            int x = width / 2 - 100;
            int y = Math.min((height / 2 + reasonHeight / 2) + 32, height - 30);

            reconnectBtn = addDrawableChild(
                new ButtonWidget.Builder(Text.literal(getText()), button -> tryConnecting())
                    .position(x, y)
                    .size(200, 20)
                    .build()
            );

            addDrawableChild(
                new ButtonWidget.Builder(Text.literal("Toggle Auto Reconnect"), button -> {
                    autoReconnect.toggle();
                    reconnectBtn.setMessage(Text.literal(getText()));
                    time = autoReconnect.time.get() * 20;
                })
                    .position(x, y + 23)
                    .size(200, 20)
                    .build()
            );
        }
    }

    @Override
    public void tick() {
        if (!autoReconnect.isActive() || autoReconnect.lastServerAddr == null) return;

        if (time <= 0) {
            if (autoReconnect.autoReconnect.get()) tryConnecting();
        } else {
            time--;
            if (reconnectBtn != null) reconnectBtn.setMessage(Text.literal(getText()));
        }
    }

    private String getText() {
        String reconnectText = "Reconnect";
        if (autoReconnect.autoReconnect.get()) reconnectText += " " + String.format("(%.1f)", time / 20); reconnectText += " " + String.format("(%.1f)", time / 20);
        return reconnectText;
    }

    private void tryConnecting() {
        var conn = autoReconnect.lastServerAddr;
        var addr = conn.getAddress().getHostName();
        ConnectScreen.connect(new TitleScreen(), mc, ServerAddress.parse(addr), new ServerInfo("AutoReconnect", addr, false));
    }
}
