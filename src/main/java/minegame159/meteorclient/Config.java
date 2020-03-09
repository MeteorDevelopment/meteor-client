package minegame159.meteorclient;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.utils.Vector2;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Config {
    public static Config INSTANCE;
    private static final File file = new File(MeteorClient.directory, "config.json");

    public String prefix = ".";

    public Map<Category, Vector2> guiPositions = new HashMap<>();

    static {
        file.getParentFile().mkdirs();
    }

    public static void save() {
        try {
            Writer writer = new FileWriter(file);
            MeteorClient.gson.toJson(INSTANCE, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if (!file.exists()) {
            if (INSTANCE == null) INSTANCE = new Config();
            return;
        }

        try {
            FileReader reader = new FileReader(file);
            INSTANCE = MeteorClient.gson.fromJson(reader, Config.class);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
