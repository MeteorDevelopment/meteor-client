/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.Reconnect;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Unique private GridWidget grid;
    @Unique private ButtonWidget reconnectBtn;
    @Unique private double time = Modules.get().get(Reconnect.class).time.get() * 20;
    @Unique
    Reconnect reconnect = Modules.get().get(Reconnect.class);

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/GridWidget;refreshPositions()V", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onInit_before(CallbackInfo info, GridWidget.Adder adder) {
        if (reconnect.isActive() && reconnect.lastServerInfo == null) return;

        grid = new GridWidget();
        adder.add(grid);

        grid.setRowSpacing(2);
        GridWidget.Adder myAdder = grid.createAdder(1);

        reconnectBtn = myAdder.add(new ButtonWidget.Builder(getText(), button -> reconnect()).build());

        myAdder.add(
            new ButtonWidget.Builder(Text.literal("Toggle Auto Reconnect"), button -> {
                reconnect.autoReconnect.set(!reconnect.autoReconnect.get());
                reconnectBtn.setMessage(getText());
                time = reconnect.time.get() * 20;
            }).build()
        );
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/GridWidget;refreshPositions()V", shift = At.Shift.AFTER))
    private void onInit_after(CallbackInfo info) {
        if (grid != null) {
            grid.refreshPositions();
            grid.forEachChild(this::addDrawableChild);
        }
    }

    @Override
    public void tick() {
        Reconnect reconnect = Modules.get().get(Reconnect.class);
        if (!reconnect.isActive() || reconnect.lastServerInfo == null) return;

        if (time <= 0) {
            if (reconnect.autoReconnect.get()) reconnect();
        } else {
            time--;
            if (reconnectBtn != null) reconnectBtn.setMessage(getText());
        }
    }

    private MutableText getText() {
        String reconnectText = "Reconnect";
        if (Modules.get().isActive(Reconnect.class)) reconnectText += " " + String.format("(%.1f)", time / 20);
        return Text.literal(reconnectText);
    }

    private void reconnect() {
        ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), client, ServerAddress.parse(reconnect.lastServerInfo.address), reconnect.lastServerInfo, false);
    }
}
