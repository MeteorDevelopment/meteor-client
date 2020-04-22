package minegame159.meteorclient.gui.screens;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.MacroListChangedEvent;
import minegame159.meteorclient.macros.Macro;
import minegame159.meteorclient.macros.MacroManager;
import minegame159.meteorclient.gui.widgets.*;

public class MacrosScreen extends WindowScreen implements Listenable {
    public MacrosScreen() {
        super("Macros", true);

        initWidgets();
    }

    @Override
    protected void init() {
        super.init();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private void initWidgets() {
        // Macros
        if (MacroManager.INSTANCE.getAll().size() > 0) {
            WTable table = add(new WTable()).getWidget();
            row();

            for (Macro macro : MacroManager.INSTANCE.getAll()) {
                table.add(new WLabel(macro.name));

                WButton edit = table.add(new WButton("Edit")).getWidget();
                edit.action = button -> mc.openScreen(new EditMacroScreen(macro));

                WMinus remove = table.add(new WMinus()).getWidget();
                remove.action = minus -> MacroManager.INSTANCE.remove(macro);

                table.row();
            }

            add(new WHorizontalSeparator());
            row();
        }

        // Add
        WPlus add = add(new WPlus()).fillX().right().getWidget();
        add.action = plus -> mc.openScreen(new EditMacroScreen(null));
    }

    @EventHandler
    private Listener<MacroListChangedEvent> onMacroListChanged = new Listener<>(event -> {
        clear();
        initWidgets();
    });

    @Override
    public void onClose() {
        MeteorClient.EVENT_BUS.unsubscribe(this);
        super.onClose();
    }
}
