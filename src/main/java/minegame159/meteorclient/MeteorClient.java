/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
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
import minegame159.meteorclient.events.meteor.KeyEvent;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.gui.screens.topbar.TopBarModules;
import minegame159.meteorclient.macros.MacroManager;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.misc.DiscordPresence;
import minegame159.meteorclient.rendering.Fonts;
import minegame159.meteorclient.rendering.MFont;
import minegame159.meteorclient.utils.*;
import minegame159.meteorclient.waypoints.Waypoints;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;

import java.io.File;
import java.time.LocalDateTime;

public class MeteorClient implements ClientModInitializer, Listenable {
    public static MeteorClient INSTANCE;
    public static final EventBus EVENT_BUS = new EventManager();
    public static MFont FONT_2X;
    public static boolean IS_DISCONNECTING;
    public static final File FOLDER = new File(FabricLoader.getInstance().getGameDir().toString(), "meteor-client");

    private MinecraftClient mc;

    public Screen screenToOpen;

    @Override
    public void onInitializeClient() {
        if (INSTANCE == null) {
            KeyBinds.Register();

            INSTANCE = this;
            return;
        }

        System.out.println("Initializing Meteor Client.");

        mc = MinecraftClient.getInstance();
        Utils.mc = mc;
        EntityUtils.mc = mc;

        Config.INSTANCE = new Config();
        Config.INSTANCE.load();
        Fonts.init();

        MeteorExecutor.init();
        new ModuleManager();
        CommandManager.init();
        EChestMemory.init();
        Capes.init();
        BlockIterator.init();

        load();
        Ignore.load();
        Waypoints.loadIcons();

        EVENT_BUS.subscribe(this);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            save();
            OnlinePlayers.leave();
        }));
    }

    public void load() {
        if (!ModuleManager.INSTANCE.load()) {
            ModuleManager.INSTANCE.get(DiscordPresence.class).toggle(false);
            Utils.addMeteorPvpToServerList();
        }

        FriendManager.INSTANCE.load();
        MacroManager.INSTANCE.load();
        AccountManager.INSTANCE.load();
    }

    public void save() {
        System.out.println("[Meteor] Saving");

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

    private boolean done = false;
    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        Capes.tick();

        if (screenToOpen != null && mc.currentScreen == null) {
            mc.openScreen(screenToOpen);
            screenToOpen = null;
        }

        if (Utils.canUpdate()) {
            mc.player.getActiveStatusEffects().values().removeIf(statusEffectInstance -> statusEffectInstance.getDuration() <= 0);
        }

        if (mc.world != null && mc.player != null && !done) {
            LocalDateTime date1 = LocalDateTime.now();
            if (date1.isAfter(LocalDateTime.of(2021, 1, 1, 0, 0, 0)) && date1.isBefore(LocalDateTime.of(2021, 1, 1, 1, 0, 0))) {
                done = true;
                Chat.info("%s%sH%sa%sp%sp%sy %sN%se%sw %sY%se%sa%sr%s!", Formatting.UNDERLINE, Formatting.DARK_RED, Formatting.RED, Formatting.GOLD, Formatting.YELLOW, Formatting.GREEN, Formatting.DARK_GREEN, Formatting.AQUA, Formatting.DARK_AQUA, Formatting.BLUE, Formatting.DARK_PURPLE, Formatting.LIGHT_PURPLE, Formatting.WHITE, Formatting.BLACK);
                mc.world.playSound(mc.player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1f, 1f, false);
            }
        }
    });

    @EventHandler
    private final Listener<KeyEvent> onKey = new Listener<>(event -> {
        // Click GUI
        if (event.action == KeyAction.Press && event.key == KeyBindingHelper.getBoundKeyOf(KeyBinds.OPEN_CLICK_GUI).getCode()) {
            if (!Utils.canUpdate() || mc.currentScreen == null) openClickGui();
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
