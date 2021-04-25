/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.misc.AutoReconnect;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public class DisconnectedScreenMixin extends ScreenMixin {

    @Shadow private int reasonHeight;

    private ButtonWidget reconnectBtn;
    private long reconnectTime;

    @Inject(method = "init", at = @At("TAIL"))
    private void onRenderBackground(CallbackInfo info) {
        AutoReconnect module = Modules.get().get(AutoReconnect.class);

        if (module.lastServerInfo != null) {
            int x = width / 2 - 100;
            int y = Math.min((height / 2 + reasonHeight / 2) + 32, height - 30);

            reconnectTime = (long) (System.currentTimeMillis() + (Modules.get().get(AutoReconnect.class).time.get() * 1000));

            String reconnectText = "Reconnect";
            if (module.isActive()) reconnectText += " " + getTimeString();

            reconnectBtn = addButton(new ButtonWidget(x, y, 200, 20, new LiteralText(reconnectText), button -> reconnect()));
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (reconnectBtn == null) return;

        if (Modules.get().isActive(AutoReconnect.class)) {
            if (reconnectTime-- - System.currentTimeMillis() > 0) ((AbstractButtonWidgetAccessor) reconnectBtn).setText(new LiteralText("Reconnect " + getTimeString()));
            else reconnect();
        }
    }

    private void reconnect() {
        Utils.mc.openScreen(new ConnectScreen(new MultiplayerScreen(new TitleScreen()), Utils.mc, Modules.get().get(AutoReconnect.class).lastServerInfo));
    }

    private String getTimeString() {
        return String.format("(%.1f)", (reconnectTime - System.currentTimeMillis()) / 1000D);
    }
}
