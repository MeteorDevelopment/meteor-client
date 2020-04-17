package minegame159.meteorclient;

import me.zero.alpine.bus.EventBus;
import me.zero.alpine.bus.EventManager;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.accountsfriends.AccountManager;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.font.CFontRenderer;
import minegame159.meteorclient.macros.MacroManager;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.newgui.WidgetScreen;
import minegame159.meteorclient.newgui.widgets.WLabel;
import minegame159.meteorclient.newgui.widgets.WTable;
import minegame159.meteorclient.utils.EntityUtils;
import minegame159.meteorclient.utils.Utils;
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
    public static CFontRenderer TEXT_RENDERER;
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

        ModuleManager.INSTANCE = new ModuleManager();
        CommandManager.init();
        AccountManager.init();

        load();

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
    }

    private void openClickGui() {
        //mc.openScreen(new ClickGUI());

        WidgetScreen screen = new WidgetScreen("Test");
        WTable table = (WTable) screen.root.add(new WTable()).centerXY().getWidget();
        table.pad(6);
        table.defaultCell.spaceVertical(4);

        table.add(new WLabel("Test", true)).fillX().centerXY();
        table.row();
        table.add(new WLabel("Sample text."));
        table.add(new WLabel("Another text in the same row."));
        table.row();
        table.add(new WLabel("SADNIbaskjdlanh D")).fillX().right();
        table.row();

        WTable table2 = (WTable) table.add(new WTable()).fillX().expandX().getWidget();
        table2.add(new WLabel("A")).fillX().centerX();
        table2.add(new WLabel("B")).fillX().centerX();
        table2.add(new WLabel("C")).fillX().centerX();

        mc.openScreen(screen);
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (openClickGui.isPressed() && mc.currentScreen == null) {
            openClickGui();
        }
    });

    private void loadFont() {
        File[] files = FOLDER.exists() ? FOLDER.listFiles() : new File[0];
        File fontFile = null;
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".ttf")) {
                    fontFile = file;
                    break;
                }
            }
        }

        if (fontFile == null) {
            try {
                fontFile = new File(FOLDER, "Comfortaa.ttf");
                fontFile.getParentFile().mkdirs();

                InputStream in = MeteorClient.class.getResourceAsStream("/assets/meteor-client/Comfortaa.ttf");
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
            TEXT_RENDERER = new CFontRenderer(Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(18f), true, true);
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
