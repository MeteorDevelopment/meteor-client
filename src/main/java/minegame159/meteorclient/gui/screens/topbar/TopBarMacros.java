package minegame159.meteorclient.gui.screens.topbar;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.MacroListChangedEvent;
import minegame159.meteorclient.gui.TopBarType;
import minegame159.meteorclient.gui.screens.EditMacroScreen;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.macros.Macro;
import minegame159.meteorclient.macros.MacroManager;

public class TopBarMacros extends TopBarScreen implements Listenable {
    private WWindow window;

    public TopBarMacros() {
        super(TopBarType.Macros);

        window = add(new WWindow(title, true)).centerXY().getWidget();

        initWidgets();
    }

    @Override
    public void clear() {
        window.clear();
    }

    @Override
    protected void init() {
        super.init();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private void initWidgets() {
        // Macros
        if (MacroManager.INSTANCE.getAll().size() > 0) {
            WTable table = window.add(new WTable()).getWidget();
            window.row();

            for (Macro macro : MacroManager.INSTANCE.getAll()) {
                table.add(new WLabel(macro.name));

                WButton edit = table.add(new WButton("Edit")).getWidget();
                edit.action = button -> mc.openScreen(new EditMacroScreen(macro));

                WMinus remove = table.add(new WMinus()).getWidget();
                remove.action = minus -> MacroManager.INSTANCE.remove(macro);

                table.row();
            }

            window.add(new WHorizontalSeparator());
            window.row();
        }

        // Add
        WPlus add = window.add(new WPlus()).fillX().right().getWidget();
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
