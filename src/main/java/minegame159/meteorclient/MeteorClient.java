package minegame159.meteorclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.BaseMinecraftSessionService;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import com.mojang.util.UUIDTypeAdapter;
import me.zero.alpine.bus.EventBus;
import me.zero.alpine.bus.EventManager;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.altsfriends.FriendManager;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.events.GameDisconnectedEvent;
import minegame159.meteorclient.events.GameJoinedEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.gui.clickgui.ClickGUI;
import minegame159.meteorclient.json.GameProfileSerializer;
import minegame159.meteorclient.json.ModuleManagerSerializer;
import minegame159.meteorclient.json.ModuleSerializer;
import minegame159.meteorclient.json.SettingSerializer;
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
import net.minecraft.client.util.Session;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;

public class MeteorClient implements ClientModInitializer, Listenable {
    public static MeteorClient INSTANCE;
    public static EventBus eventBus = new EventManager();
    public static Gson gson;

    public static File directory = new File(FabricLoader.getInstance().getGameDirectory(), "meteor-client");

    private MinecraftClient mc;
    private FabricKeyBinding openClickGui = FabricKeyBinding.Builder.create(new Identifier("meteor-client", "open-click-gui"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.misc").build();
    private YggdrasilUserAuthentication userAuthentication;
    private Field sessionField;

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

        try {
            Field authenticationServiceField = BaseMinecraftSessionService.class.getDeclaredField("authenticationService");
            authenticationServiceField.setAccessible(true);
            userAuthentication = (YggdrasilUserAuthentication) ((YggdrasilAuthenticationService) authenticationServiceField.get(mc.getSessionService())).createUserAuthentication(Agent.MINECRAFT);

            sessionField = MinecraftClient.class.getDeclaredField("session");
            sessionField.setAccessible(true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        KeyBindingRegistry.INSTANCE.register(openClickGui);

        eventBus.subscribe(this);
    }

    public boolean logIn(String username, String password) {
        if (userAuthentication.isLoggedIn()) userAuthentication.logOut();
        userAuthentication.setUsername(username);
        userAuthentication.setPassword(password);
        try {
            userAuthentication.logIn();
            GameProfile profile = userAuthentication.getSelectedProfile();
            sessionField.set(mc, new Session(profile.getName(), profile.getId().toString(), userAuthentication.getAuthenticatedToken(), userAuthentication.getUserType().getName()));
            return true;
        } catch (AuthenticationException | IllegalAccessException e) {
            return false;
        }
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
        FriendManager.load();
    });

    @EventHandler
    private Listener<GameDisconnectedEvent> onGameDisconnected = new Listener<>(event -> {
        Config.save();
        ModuleManager.save();
    });
}
