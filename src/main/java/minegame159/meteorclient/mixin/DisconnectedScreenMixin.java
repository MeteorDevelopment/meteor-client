/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IAbstractButtonWidget;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.misc.AutoReconnect;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public class DisconnectedScreenMixin extends ScreenMixin {

    @Shadow
    private int reasonHeight;

    private ButtonWidget reconnectBtn;
    private boolean timerActive = true;
    private double time = ((AutoReconnect) ModuleManager.INSTANCE.get(AutoReconnect.class)).time.get() * 20;

    @Inject(method = "init", at = @At("HEAD"))
    private void onRenderBackground(CallbackInfo info) {
        reconnectBtn = super.addButton(new ButtonWidget(width / 2 - 100, height / 2 + reasonHeight / 2 + 9 + 30, 200,
                20, new LiteralText("Reconnecting in " + time / 20f), button -> timerActive = !timerActive));
        timerActive = ModuleManager.INSTANCE.isActive(AutoReconnect.class);
    }

    @Override
    public void tick() {
        if (timerActive) {
            time--;
            if (time <= 0) {
                Utils.mc.openScreen(new ConnectScreen(new MultiplayerScreen(new TitleScreen()), Utils.mc,
                        ((AutoReconnect) ModuleManager.INSTANCE.get(AutoReconnect.class)).lastServerInfo));

            } else {
                ((IAbstractButtonWidget) reconnectBtn)
                        .setText(new LiteralText(String.format("Reconnecting in %.1f", time / 20f)));
            }
        }
    }
}
