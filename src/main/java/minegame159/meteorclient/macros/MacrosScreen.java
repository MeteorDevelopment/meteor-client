package minegame159.meteorclient.macros;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.MacroListChangedEvent;
import minegame159.meteorclient.gui.Alignment;
import minegame159.meteorclient.gui.PanelListScreen;
import minegame159.meteorclient.gui.widgets.*;

public class MacrosScreen extends PanelListScreen implements Listenable {
    public MacrosScreen() {
        super("Macros");

        initWidgets();

        MeteorClient.eventBus.subscribe(this);
    }

    private void initWidgets() {
        // Macros
        if (MacroManager.INSTANCE.getAll().size() > 0) {
            WGrid grid = add(new WGrid(4, 4, 3));
            for (Macro macro : MacroManager.INSTANCE.getAll()) {
                WLabel name = new WLabel(macro.name);
                WButton edit = new WButton("Edit");
                edit.action = () -> mc.openScreen(new EditMacroScreen(macro));
                WMinus remove = new WMinus();
                remove.action = () -> MacroManager.INSTANCE.remove(macro);

                grid.addRow(name, edit, remove);
            }
            add(new WHorizontalSeparator());
        }

        // Add
        WPlus add = add(new WPlus());
        add.boundingBox.alignment.x = Alignment.X.Right;
        add.action = () -> mc.openScreen(new EditMacroScreen(null));

        layout();
    }

    @EventHandler
    private Listener<MacroListChangedEvent> onMacroListChanged = new Listener<>(event -> {
        clear();
        initWidgets();
    });

    @Override
    public void onClose() {
        MeteorClient.eventBus.unsubscribe(this);
        super.onClose();
    }
}
