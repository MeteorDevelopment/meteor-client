/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient;

import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.IEventBus;
import minegame159.meteorclient.events.game.GameLeftEvent;
import minegame159.meteorclient.events.meteor.ClientInitialisedEvent;
import minegame159.meteorclient.events.meteor.KeyEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.screens.topbar.TopBarModules;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.misc.DiscordPresence;
import minegame159.meteorclient.modules.render.hud.HudEditorScreen;
import minegame159.meteorclient.rendering.Fonts;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.rendering.text.CustomTextRenderer;
import minegame159.meteorclient.systems.Systems;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.misc.MeteorPlayers;
import minegame159.meteorclient.utils.misc.Names;
import minegame159.meteorclient.utils.misc.input.KeyAction;
import minegame159.meteorclient.utils.misc.input.KeyBinds;
import minegame159.meteorclient.utils.network.Capes;
import minegame159.meteorclient.utils.network.MeteorExecutor;
import minegame159.meteorclient.utils.network.OnlinePlayers;
import minegame159.meteorclient.utils.player.EChestMemory;
import minegame159.meteorclient.utils.player.Rotations;
import minegame159.meteorclient.utils.render.color.RainbowColors;
import minegame159.meteorclient.utils.world.BlockIterator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class MeteorClient implements ClientModInitializer {
    public static MeteorClient INSTANCE;
    public static final IEventBus EVENT_BUS = new EventBus();
    public static final File FOLDER = new File(FabricLoader.getInstance().getGameDir().toString(), "meteor-client");
    public static final Logger LOG = LogManager.getLogger();

    public static CustomTextRenderer FONT;

    private MinecraftClient mc;

    public Screen screenToOpen;

    @Override
    public void onInitializeClient() {
        if (INSTANCE == null) {
            KeyBinds.Register();

            INSTANCE = this;
            return;
        }

        LOG.info("Initializing Meteor Client");

        mc = MinecraftClient.getInstance();
        Utils.mc = mc;
        EntityUtils.mc = mc;

        Systems.addPreLoadTask(() -> {
            if (!Modules.get().getFile().exists()) {
                Modules.get().get(DiscordPresence.class).toggle(false);
                Utils.addMeteorPvpToServerList();
            }
        });

        Matrices.begin(new MatrixStack());
        Fonts.init();
        MeteorExecutor.init();
        Capes.init();
        RainbowColors.init();
        BlockIterator.init();
        EChestMemory.init();
        Rotations.init();
        Names.init();
        MeteorPlayers.init();

        Systems.init();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Systems.save();
            OnlinePlayers.leave();
        }));

        EVENT_BUS.subscribe(this);
        EVENT_BUS.post(new ClientInitialisedEvent());
    }

    private void openClickGui() {
        mc.openScreen(new TopBarModules());
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        Systems.save();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        Capes.tick();

        if (screenToOpen != null && mc.currentScreen == null) {
            mc.openScreen(screenToOpen);
            screenToOpen = null;
        }

        if (Utils.canUpdate()) {
            mc.player.getActiveStatusEffects().values().removeIf(statusEffectInstance -> statusEffectInstance.getDuration() <= 0);
        }
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        // Click GUI
        if (event.action == KeyAction.Press && event.key == KeyBindingHelper.getBoundKeyOf(KeyBinds.OPEN_CLICK_GUI).getCode()) {
            if ((!Utils.canUpdate() && !(mc.currentScreen instanceof WidgetScreen) && !(mc.currentScreen instanceof HudEditorScreen)) || mc.currentScreen == null) openClickGui();
        }

        // Shulker Peek
        KeyBinding shulkerPeek = KeyBinds.SHULKER_PEEK;
        shulkerPeek.setPressed(shulkerPeek.matchesKey(event.key, 0) && event.action != KeyAction.Release);
    }
}
