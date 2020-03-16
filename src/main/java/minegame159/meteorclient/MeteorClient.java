package minegame159.meteorclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import com.mojang.util.UUIDTypeAdapter;
import me.zero.alpine.bus.EventBus;
import me.zero.alpine.bus.EventManager;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.altsfriends.AccountManager;
import minegame159.meteorclient.altsfriends.FriendManager;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.gui.clickgui.ClickGUI;
import minegame159.meteorclient.json.GameProfileSerializer;
import minegame159.meteorclient.json.ModuleManagerSerializer;
import minegame159.meteorclient.json.ModuleSerializer;
import minegame159.meteorclient.json.SettingSerializer;
import minegame159.meteorclient.macros.MacroManager;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.settings.Setting;
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
import java.lang.reflect.Modifier;
import java.util.UUID;

public class MeteorClient implements ClientModInitializer, Listenable {
    public static MeteorClient INSTANCE;
    public static EventBus eventBus = new EventManager();
    public static Gson gson;

    public static File directory = new File(FabricLoader.getInstance().getGameDirectory(), "meteor-client");

    private MinecraftClient mc;
    private FabricKeyBinding openClickGui = FabricKeyBinding.Builder.create(new Identifier("meteor-client", "open-click-gui"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.misc").build();

    @Override
    public void onInitializeClient() {
        if (INSTANCE == null) {
            INSTANCE = this;
            return;
        }

        System.out.println("Initializing Meteor Client.");

        mc = MinecraftClient.getInstance();
        Utils.mc = mc;
        EntityUtils.mc = mc;

        gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
                .registerTypeAdapter(ModuleManager.class, new ModuleManagerSerializer())
                .registerTypeAdapter(Module.class, new ModuleSerializer())
                .registerTypeAdapter(Setting.class, new SettingSerializer())
                .registerTypeAdapter(GameProfile.class, new GameProfileSerializer())
                .registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer())
                .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
                .registerTypeAdapter(ProfileSearchResultsResponse.class, new ProfileSearchResultsResponse.Serializer())
                .setPrettyPrinting()
                .create();

        MixinValues.init();
        CommandManager.init();
        AccountManager.init();

        SaveManager.register(Config.class, "config");
        SaveManager.register(ModuleManager.class, "modules");
        SaveManager.register(FriendManager.class, "friends");
        SaveManager.register(MacroManager.class, "macros");
        SaveManager.register(AccountManager.class, "accounts");

        SaveManager.load(Config.class);
        SaveManager.load(ModuleManager.class);
        SaveManager.load(FriendManager.class);
        SaveManager.load(MacroManager.class);
        SaveManager.load(AccountManager.class);

        KeyBindingRegistry.INSTANCE.register(openClickGui);
        eventBus.subscribe(this);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    private void stop() {
        SaveManager.save(Config.class);
        SaveManager.save(ModuleManager.class);
        SaveManager.save(FriendManager.class);
        SaveManager.save(MacroManager.class);
        SaveManager.save(AccountManager.class);
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (openClickGui.isPressed() && mc.currentScreen == null) {
            mc.openScreen(new ClickGUI());
        }
    });

    public void onKeyInMainMenu(int key) {
        if (key == openClickGui.getBoundKey().getKeyCode()) {
            ClickGUI clickGUI = new ClickGUI();
            clickGUI.parent = mc.currentScreen;
            mc.openScreen(clickGUI);
        }
    }
}
