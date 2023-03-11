/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IDisconnectedScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen implements IDisconnectedScreen {
    @Shadow
    private int reasonHeight;

    @Unique
    private ButtonWidget reconnectBtn;
    @Unique
    private double time = Modules.get().get(AutoReconnect.class).time.get() * 20;

    private final AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onRenderBackground(CallbackInfo info) {
        if (autoReconnect.isActive()) {
            int x = width / 2 - 100;
            int y = Math.min((height / 2 + reasonHeight / 2) + 32, height - 30);

            reconnectBtn = addDrawableChild(
                new ButtonWidget.Builder(Text.literal(getText()), button -> reconnect())
                    .position(x, y)
                    .size(200, 20)
                    .build()
            );

            addDrawableChild(
                new ButtonWidget.Builder(Text.literal("Toggle Auto Reconnect"), button -> {
                    autoReconnect.autoReconnect.set(!autoReconnect.autoReconnect.get());
                    ((AbstractButtonWidgetAccessor) reconnectBtn).setText(Text.literal(getText()));
                    time = Modules.get().get(AutoReconnect.class).time.get() * 20;
                })
                    .position(x, y + 22)
                    .size(200, 20)
                    .build()
            );
        }
    }

    @Override
    public void tickScreen() {
        if (!autoReconnect.isActive()) return;
        if (time <= 0) {
            if (autoReconnect.autoReconnect.get()) reconnect();
        } else {
            time--;
            if (reconnectBtn != null) ((AbstractButtonWidgetAccessor) reconnectBtn).setText(Text.literal(getText()));
        }
    }

    @Unique
    private void reconnect() {
        if (autoReconnect.lastServerInfo == null) return;
        ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), client,
            ServerAddress.parse(autoReconnect.lastServerInfo.address), autoReconnect.lastServerInfo);
    }

    @Unique
    private String getText() {
        String reconnectText = "Reconnect";
        if (autoReconnect.autoReconnect.get()) reconnectText += " " + String.format("(%.1f)", time / 20);
        return reconnectText;
    }
}
