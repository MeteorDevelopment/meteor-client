/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient;

import me.zero.alpine.bus.EventBus;
import me.zero.alpine.bus.EventManager;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.accounts.AccountManager;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.commands.commands.Ignore;
import minegame159.meteorclient.events.game.GameLeftEvent;
import minegame159.meteorclient.events.meteor.ClientInitialisedEvent;
import minegame159.meteorclient.events.meteor.KeyEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.screens.topbar.TopBarModules;
import minegame159.meteorclient.macros.MacroManager;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.misc.DiscordPresence;
import minegame159.meteorclient.rendering.Fonts;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.rendering.text.CustomTextRenderer;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.misc.input.KeyAction;
import minegame159.meteorclient.utils.misc.input.KeyBinds;
import minegame159.meteorclient.utils.network.Capes;
import minegame159.meteorclient.utils.network.MeteorExecutor;
import minegame159.meteorclient.utils.network.OnlinePlayers;
import minegame159.meteorclient.utils.player.EChestMemory;
import minegame159.meteorclient.utils.player.RotationUtils;
import minegame159.meteorclient.utils.render.color.RainbowColorManager;
import minegame159.meteorclient.utils.world.BlockIterator;
import minegame159.meteorclient.waypoints.Waypoints;
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

public class MeteorClient implements ClientModInitializer, Listenable {
    public static MeteorClient INSTANCE;
    public static final EventBus EVENT_BUS = new EventManager();
    public static boolean IS_DISCONNECTING;
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

        Config.INSTANCE = new Config();
        Config.INSTANCE.load();
        Fonts.init();

        Matrices.begin(new MatrixStack());
        MeteorExecutor.init();
        new ModuleManager();
        CommandManager.init();
        EChestMemory.init();
        Capes.init();
        BlockIterator.init();
        RainbowColorManager.init();
        RotationUtils.init();

        load();
        Ignore.load();
        Waypoints.loadIcons();

        EVENT_BUS.subscribe(this);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            save();
            OnlinePlayers.leave();
        }));
        EVENT_BUS.post(new ClientInitialisedEvent());
    }

    public void load() {
        LOG.info("Loading");

        if (!ModuleManager.INSTANCE.load()) {
            ModuleManager.INSTANCE.get(DiscordPresence.class).toggle(false);
            Utils.addMeteorPvpToServerList();
        }

        FriendManager.INSTANCE.load();
        MacroManager.INSTANCE.load();
        AccountManager.INSTANCE.load();
    }

    public void save() {
        LOG.info("Saving");

        Config.INSTANCE.save();
        ModuleManager.INSTANCE.save();
        FriendManager.INSTANCE.save();
        MacroManager.INSTANCE.save();
        AccountManager.INSTANCE.save();

        Ignore.save();
    }

    private void openClickGui() {
        mc.openScreen(new TopBarModules());
    }

    @EventHandler
    private final Listener<GameLeftEvent> onGameLeft = new Listener<>(event -> save());

    @EventHandler
    private final Listener<TickEvent.Post> onTick = new Listener<>(event -> {
        Capes.tick();

        if (screenToOpen != null && mc.currentScreen == null) {
            mc.openScreen(screenToOpen);
            screenToOpen = null;
        }

        if (Utils.canUpdate()) {
            mc.player.getActiveStatusEffects().values().removeIf(statusEffectInstance -> statusEffectInstance.getDuration() <= 0);
        }
    });

    @EventHandler
    private final Listener<KeyEvent> onKey = new Listener<>(event -> {
        // Click GUI
        if (event.action == KeyAction.Press && event.key == KeyBindingHelper.getBoundKeyOf(KeyBinds.OPEN_CLICK_GUI).getCode()) {
            if ((!Utils.canUpdate() && !(mc.currentScreen instanceof WidgetScreen)) || mc.currentScreen == null) openClickGui();
        }

        // Shulker Peek
        KeyBinding shulkerPeek = KeyBinds.SHULKER_PEEK;
        ((IKeyBinding) shulkerPeek).setPressed(shulkerPeek.matchesKey(event.key, 0) && event.action != KeyAction.Release);
    });

    public static <T> T postEvent(T event) {
        EVENT_BUS.post(event);
        return event;
    }
}
