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
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Unique private GridWidget grid;
    @Unique private ButtonWidget reconnectBtn;
    @Unique private double time = Modules.get().get(AutoReconnect.class).time.get() * 20;

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/GridWidget;refreshPositions()V", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onInit_before(CallbackInfo info, GridWidget.Adder adder) {
        AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);

        if (autoReconnect.lastServerConnection != null) {
            grid = new GridWidget();
            adder.add(grid);

            grid.setRowSpacing(2);
            GridWidget.Adder myAdder = grid.createAdder(1);

            reconnectBtn = myAdder.add(new ButtonWidget.Builder(Text.literal(getText()), button -> tryConnecting()).build());

            myAdder.add(
                new ButtonWidget.Builder(Text.literal("Toggle Auto Reconnect"), button -> {
                    autoReconnect.toggle();
                    reconnectBtn.setMessage(Text.literal(getText()));
                    time = autoReconnect.time.get() * 20;
                }).build()
            );
        }
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
        AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);
        if (!autoReconnect.isActive() || autoReconnect.lastServerConnection == null) return;

        if (time <= 0) {
            tryConnecting();
        } else {
            time--;
            if (reconnectBtn != null) reconnectBtn.setMessage(Text.literal(getText()));
        }
    }

    private String getText() {
        String reconnectText = "Reconnect";
        if (Modules.get().isActive(AutoReconnect.class)) reconnectText += " " + String.format("(%.1f)", time / 20);
        return reconnectText;
    }

    private void tryConnecting() {
        var conn = Modules.get().get(AutoReconnect.class).lastServerConnection;
        var host = conn.getAddress().getHostName();
        if (host.contains(":")) host = host.substring(0, host.indexOf(":"));
        ConnectScreen.connect(new TitleScreen(), mc, new ServerAddress(host, conn.getPort()), new ServerInfo(I18n.translate("selectServer.defaultName"), host, false), false);
    }
}
