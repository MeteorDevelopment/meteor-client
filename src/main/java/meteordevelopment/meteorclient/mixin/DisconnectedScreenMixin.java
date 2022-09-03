/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.systems.modules.misc.ServerSpoof;
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
public abstract class DisconnectedScreenMixin extends Screen {
    @Shadow private int reasonHeight;

    @Unique private ButtonWidget reconnectBtn;
    @Unique private double time = Modules.get().get(AutoReconnect.class).time.get() * 20;

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onRenderBackground(CallbackInfo info) {
        if (Modules.get().get(AutoReconnect.class).lastServerInfo != null) {
            int x = width / 2 - 100;
            int y = Math.min((height / 2 + reasonHeight / 2) + 32, height - 30);

            reconnectBtn = addDrawableChild(new ButtonWidget(x, y, 200, 20, Text.literal(getText()),
                button -> ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), client,
                ServerAddress.parse(Modules.get().get(AutoReconnect.class).lastServerInfo.address),
                Modules.get().get(AutoReconnect.class).lastServerInfo)));
            addDrawableChild(new ButtonWidget(x, y + 22, 200, 20, Text.literal("Toggle AutoReconnect"),
                button -> {
                    Modules.get().get(AutoReconnect.class).toggle();
                    time = Modules.get().get(AutoReconnect.class).time.get() * 20;
                }));
        }
    }

    @Override
    public void tick() {
        AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);
        if (!autoReconnect.isActive() || autoReconnect.lastServerInfo == null) return;

        if (time <= 0) {
            ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), client,
                ServerAddress.parse(autoReconnect.lastServerInfo.address), autoReconnect.lastServerInfo);
        } else {
            time--;
            if (reconnectBtn != null) ((AbstractButtonWidgetAccessor) reconnectBtn).setText(Text.literal(getText()));
        }
    }

    private String getText() {
        String reconnectText = "Reconnect";
        if (Modules.get().isActive(AutoReconnect.class)) reconnectText += " " + String.format("(%.1f)", time / 20);
        return reconnectText;
    }
}
