package minegame159.meteorclient;

import java.io.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class SaveManager {
    private static Map<Class<?>, File> files = new HashMap<>();

    public static void register(Class<?> klass, String file) {
        files.put(klass, new File(MeteorClient.directory, file + ".json"));
    }

    public static void save(Class<?> klass) {
        File file = files.get(klass);
        if (file == null) throw new IllegalArgumentException(klass + " was registered.");

        try {
            Writer writer = new FileWriter(file);
            MeteorClient.gson.toJson(klass.getField("INSTANCE").get(null), writer);
            writer.close();
        } catch (IOException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void load(Class<?> klass) {
        File file = files.get(klass);
        if (file == null) throw new IllegalArgumentException(klass + " was registered.");

        try {
            Field instance = klass.getField("INSTANCE");

            if (!file.exists()) {
                if (instance.get(null) == null) instance.set(null, klass.newInstance());
                return;
            }

            Reader reader = new FileReader(file);
            instance.set(null, MeteorClient.gson.fromJson(reader, klass));
            reader.close();

            if (instance.get(null) == null) {
                System.out.println("Meteor-Client: Failed to load " + klass + ", resetting.");
                file.delete();
                load(klass);
            }
        } catch (NoSuchFieldException | IllegalAccessException | InstantiationException | IOException e) {
            e.printStackTrace();
        }
    }
}
