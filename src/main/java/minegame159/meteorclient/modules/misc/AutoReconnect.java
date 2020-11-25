/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.ConnectToServerEvent;
import minegame159.meteorclient.events.OpenScreenEvent;
import minegame159.meteorclient.mixininterface.IAbstractButtonWidget;
import minegame159.meteorclient.mixininterface.IDisconnectedScreen;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.LiteralText;

public class AutoReconnect extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Double> time = sgGeneral.add(new DoubleSetting.Builder()
            .name("time")
            .description("Time to wait before connecting.")
            .defaultValue(2.0)
            .min(0.0)
            .build()
    );

    private ServerInfo lastServerInfo;

    public AutoReconnect() {
        super(Category.Misc, "auto-reconnect", "Automatically reconnects when kicked from server.");
        MeteorClient.EVENT_BUS.subscribe(new Listener<ConnectToServerEvent>(event -> {
            lastServerInfo = mc.isInSingleplayer() ? null : mc.getCurrentServerEntry();
        }));
    }

    @EventHandler
    private final Listener<OpenScreenEvent> onOpenScreen = new Listener<>(event -> {
        if (lastServerInfo == null) return;
        if (!(event.screen instanceof DisconnectedScreen)) return;
        if (event.screen instanceof AutoReconnectScreen) return;

        mc.openScreen(new AutoReconnectScreen((DisconnectedScreen) event.screen));
        event.cancel();
    });

    private class AutoReconnectScreen extends DisconnectedScreen {
        private final int reasonHeight;

        private ButtonWidget reconnectBtn;
        private boolean timerActive = true;
        private int timer;

        public AutoReconnectScreen(DisconnectedScreen screen) {
            super(((IDisconnectedScreen) screen).getParent(), screen.getTitle(), ((IDisconnectedScreen) screen).getReason());
            reasonHeight = ((IDisconnectedScreen) screen).getReasonHeight();
            timer = (int) (time.get() * 20);
        }

        @Override
        protected void init() {
            super.init();
            reconnectBtn = addButton(new ButtonWidget(width / 2 - 100, height / 2 + reasonHeight / 2 + 9 + 30, 200, 20, new LiteralText("Reconnecting in " + timer / 20f), button -> timerActive = !timerActive));
        }

        @Override
        public void tick() {
            super.tick();

            if (timer <= 0) {
                client.openScreen(new ConnectScreen(new MultiplayerScreen(new TitleScreen()), client, lastServerInfo));
            }

            if (timerActive) {
                timer--;
                ((IAbstractButtonWidget) reconnectBtn).setText(new LiteralText(String.format("Reconnecting in %.1f", timer / 20f)));
            }
        }
    }
}
