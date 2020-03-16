package minegame159.meteorclient.macros;

import me.zero.alpine.listener.Listenable;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.SaveManager;
import minegame159.meteorclient.events.EventStore;

import java.util.ArrayList;
import java.util.List;

public class MacroManager implements Listenable {
    public static MacroManager INSTANCE;

    private List<Macro> macros = new ArrayList<>();

    public void add(Macro macro) {
        macros.add(macro);
        MeteorClient.eventBus.subscribe(macro);
        MeteorClient.eventBus.post(EventStore.macroListChangedEvent());
        SaveManager.save(getClass());
    }

    public List<Macro> getAll() {
        return macros;
    }

    public void remove(Macro macro) {
        if (macros.remove(macro)) {
            MeteorClient.eventBus.unsubscribe(macro);
            MeteorClient.eventBus.post(EventStore.macroListChangedEvent());
            SaveManager.save(getClass());
        }
    }

    private void onLoad() {
        for (Macro macro : macros) MeteorClient.eventBus.subscribe(macro);
    }
}
