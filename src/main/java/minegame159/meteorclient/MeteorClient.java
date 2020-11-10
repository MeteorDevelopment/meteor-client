package minegame159.meteorclient;

import me.zero.alpine.bus.EventBus;
import me.zero.alpine.bus.EventManager;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.accounts.AccountManager;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.commands.commands.Ignore;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.gui.GuiKeyEvents;
import minegame159.meteorclient.gui.screens.topbar.TopBarModules;
import minegame159.meteorclient.macros.MacroManager;
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

import java.io.File;

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

        load();
        Ignore.load();
        Waypoints.loadIcons();

        EVENT_BUS.subscribe(this);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
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

    private void stop() {
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
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        Capes.tick();

        if (screenToOpen != null && mc.currentScreen == null) {
            mc.openScreen(screenToOpen);
            screenToOpen = null;
        }

        if (KeyBinds.OPEN_CLICK_GUI.isPressed() && mc.currentScreen == null && GuiKeyEvents.postKeyEvents()) {
            openClickGui();
        }

        mc.player.getActiveStatusEffects().values().removeIf(statusEffectInstance -> statusEffectInstance.getDuration() <= 0);
    });

    public void onKeyInMainMenu(int key) {
        if (key == KeyBindingHelper.getBoundKeyOf(KeyBinds.OPEN_CLICK_GUI).getCode()) {
            openClickGui();
        }
    }
}
