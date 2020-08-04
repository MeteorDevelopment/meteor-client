package minegame159.meteorclient;

import me.zero.alpine.bus.EventBus;
import me.zero.alpine.bus.EventManager;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.accountsfriends.AccountManager;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.commands.commands.Ignore;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.gui.GuiThings;
import minegame159.meteorclient.gui.screens.topbar.TopBarModules;
import minegame159.meteorclient.macros.MacroManager;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.rendering.MFont;
import minegame159.meteorclient.utils.Capes;
import minegame159.meteorclient.utils.EChestMemory;
import minegame159.meteorclient.utils.EntityUtils;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.waypoints.Waypoints;
import net.arikia.dev.drpc.DiscordRPC;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.*;

public class MeteorClient implements ClientModInitializer, Listenable {
    public static MeteorClient INSTANCE;
    public static final EventBus EVENT_BUS = new EventManager();
    public static MFont FONT;
    public static boolean IS_DISCONNECTING;
    public static final File FOLDER = new File(FabricLoader.getInstance().getGameDirectory(), "meteor-client");

    private MinecraftClient mc;
    private FabricKeyBinding openClickGui = FabricKeyBinding.Builder.create(new Identifier("meteor-client", "open-click-gui"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "Meteor Client").build();
    public FabricKeyBinding shulkerPeek = FabricKeyBinding.Builder.create(new Identifier("meteor-client", "shulker-peek"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "Meteor Client").build();

    @Override
    public void onInitializeClient() {
        if (INSTANCE == null) {
            KeyBindingRegistry.INSTANCE.addCategory("Meteor Client");
            KeyBindingRegistry.INSTANCE.register(openClickGui);
            KeyBindingRegistry.INSTANCE.register(shulkerPeek);

            INSTANCE = this;
            return;
        }

        System.out.println("Initializing Meteor Client.");

        mc = MinecraftClient.getInstance();
        Utils.mc = mc;
        EntityUtils.mc = mc;

        loadFont();
        Config.INSTANCE = new Config();

        ModuleManager.INSTANCE = new ModuleManager();
        CommandManager.init();
        AccountManager.init();
        EChestMemory.init();
        Capes.init();

        load();
        Ignore.load();
        Waypoints.loadIcons();

        EVENT_BUS.subscribe(this);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void load() {
        Config.INSTANCE.load();
        ModuleManager.INSTANCE.load();
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
        DiscordRPC.discordShutdown();

        Ignore.save();
    }

    private void openClickGui() {
        mc.openScreen(new TopBarModules());
    }

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        Capes.tick();

        if (openClickGui.isPressed() && mc.currentScreen == null && GuiThings.postKeyEvents()) {
            openClickGui();
        }

        mc.player.getActiveStatusEffects().values().removeIf(statusEffectInstance -> statusEffectInstance.getDuration() <= 0);
    });

    private void loadFont() {
        File[] files = FOLDER.exists() ? FOLDER.listFiles() : new File[0];
        File fontFile = null;
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".ttf") || file.getName().endsWith(".TTF")) {
                    fontFile = file;
                    break;
                }
            }
        }

        if (fontFile == null) {
            try {
                fontFile = new File(FOLDER, "JetBrainsMono-Regular.ttf");
                fontFile.getParentFile().mkdirs();

                InputStream in = MeteorClient.class.getResourceAsStream("/assets/meteor-client/JetBrainsMono-Regular.ttf");
                OutputStream out = new FileOutputStream(fontFile);

                byte[] bytes = new byte[255];
                int read;
                while ((read = in.read(bytes)) > 0) out.write(bytes, 0, read);

                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            FONT = new MFont(Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(16f), true, true);
            Waypoints.FONT = new MFont(Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(16f * 2), true, true);
            Waypoints.FONT.scale = 0.5;
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }
    }

    public void onKeyInMainMenu(int key) {
        if (key == openClickGui.getBoundKey().getKeyCode()) {
            openClickGui();
        }
    }
}
