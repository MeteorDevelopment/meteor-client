package minegame159.meteorclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.zero.alpine.bus.EventBus;
import me.zero.alpine.bus.EventManager;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.events.GameDisconnectedEvent;
import minegame159.meteorclient.events.GameJoinedEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.gui.clickgui.ClickGUI;
import minegame159.meteorclient.json.ModuleManagerSerializer;
import minegame159.meteorclient.json.ModuleSerializer;
import minegame159.meteorclient.json.SettingSerializer;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Color;
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

import java.io.File;

public class MeteorClient implements ClientModInitializer, Listenable {
    public static MeteorClient instance;
    public static EventBus eventBus = new EventManager();
    public static Gson gson;

    public static File directory = new File(FabricLoader.getInstance().getGameDirectory(), "meteor-client");

    private static MinecraftClient mc;
    private static FabricKeyBinding openClickGui = FabricKeyBinding.Builder.create(new Identifier("meteor-client", "open-click-gui"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.misc").build();

    @Override
    public void onInitializeClient() {
        if (instance == null) {
            instance = this;
            return;
        }

        System.out.println("Initializing Meteor Client.");

        mc = MinecraftClient.getInstance();
        Utils.mc = mc;
        EntityUtils.mc = mc;

        gson = new GsonBuilder()
                .registerTypeAdapter(ModuleManager.class, new ModuleManagerSerializer())
                .registerTypeAdapter(Module.class, new ModuleSerializer())
                .registerTypeAdapter(Setting.class, new SettingSerializer())
                .setPrettyPrinting()
                .create();

        MixinValues.init();
        CommandManager.init();

        KeyBindingRegistry.INSTANCE.register(openClickGui);

        eventBus.subscribe(this);
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (openClickGui.isPressed() && !mc.isPaused()) {
            mc.openScreen(new ClickGUI());
        }
    });

    @EventHandler
    private Listener<GameJoinedEvent> onGameJoined = new Listener<>(event -> {
        Config.load();
        ModuleManager.load();
    });

    @EventHandler
    private Listener<GameDisconnectedEvent> onGameDisconnected = new Listener<>(event -> {
        Config.save();
        ModuleManager.save();
    });
}
