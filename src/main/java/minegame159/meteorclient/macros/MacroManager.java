package minegame159.meteorclient.macros;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MacroManager {
    public static MacroManager INSTANCE;
    private static final File file = new File(MeteorClient.directory, "macros.json");

    private List<Macro> macros = new ArrayList<>();

    public void add(Macro macro) {
        macros.add(macro);
        MeteorClient.eventBus.subscribe(macro);
        MeteorClient.eventBus.post(EventStore.macroListChangedEvent());
        save();
    }

    public List<Macro> getAll() {
        return macros;
    }

    public void remove(Macro macro) {
        if (macros.remove(macro)) {
            MeteorClient.eventBus.unsubscribe(macro);
            MeteorClient.eventBus.post(EventStore.macroListChangedEvent());
            save();
        }
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
            if (INSTANCE == null) INSTANCE = new MacroManager();
            return;
        }

        try {
            FileReader reader = new FileReader(file);
            INSTANCE = MeteorClient.gson.fromJson(reader, MacroManager.class);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Macro macro : INSTANCE.macros) {
            MeteorClient.eventBus.subscribe(macro);
        }
    }
}
