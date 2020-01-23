package minegame159.meteorclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import minegame159.jes.eventbuses.DefaultEventBus;
import minegame159.jes.eventbuses.EventBus;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.json.ConfigSerializer;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.EntityUtils;
import minegame159.meteorclient.utils.Utils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.*;

public class MeteorClient implements ClientModInitializer {
    public static EventBus eventBus = new DefaultEventBus();
    public static Gson gson;

    private static File configFile;

    @Override
    public void onInitializeClient() {
        System.out.println("Initializing Meteor Client.");

        Utils.mc = MinecraftClient.getInstance();
        EntityUtils.mc = MinecraftClient.getInstance();

        gson = new GsonBuilder()
                .registerTypeAdapter(Config.class, new ConfigSerializer())
                .setPrettyPrinting()
                .create();

        configFile = new File(FabricLoader.getInstance().getGameDirectory(), "meteor-client.json");

        CommandManager.init();
        ModuleManager.init();
    }

    public static void saveConfig() {
        try {
            Writer writer = new FileWriter(configFile);
            gson.toJson(Config.instance, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadConfig() {
        if (configFile.exists()) {
            try {
                Config.instance = gson.fromJson(new FileReader(configFile), Config.class);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
