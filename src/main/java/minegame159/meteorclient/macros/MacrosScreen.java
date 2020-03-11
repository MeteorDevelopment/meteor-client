package minegame159.meteorclient.macros;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.MacroListChangedEvent;
import minegame159.meteorclient.gui.Alignment;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.clickgui.WHorizontalSeparatorBigger;
import minegame159.meteorclient.gui.widgets.*;
import net.minecraft.client.MinecraftClient;

public class MacrosScreen extends WidgetScreen implements Listenable {
    private WVerticalList list;

    public MacrosScreen() {
        super("Macros");

        initWidgets();

        MeteorClient.eventBus.subscribe(this);
    }

    private void initWidgets() {
        WPanel panel = add(new WPanel());
        panel.boundingBox.setMargin(6);
        panel.boundingBox.alignment.set(Alignment.X.Center, Alignment.Y.Center);

        list = panel.add(new WVerticalList(4));
        list.maxHeight = MinecraftClient.getInstance().window.getScaledHeight() - 32;

        // Title
        list.add(new WLabel("Macros", true)).boundingBox.alignment.x = Alignment.X.Center;
        list.add(new WHorizontalSeparatorBigger());

        // Macros
        if (MacroManager.INSTANCE.getAll().size() > 0) {
            WGrid grid = list.add(new WGrid(4, 4, 3));
            for (Macro macro : MacroManager.INSTANCE.getAll()) {
                WLabel name = new WLabel(macro.name);
                WButton edit = new WButton("Edit");
                edit.action = () -> mc.openScreen(new EditMacroScreen(macro));
                WMinus remove = new WMinus();
                remove.action = () -> MacroManager.INSTANCE.remove(macro);

                grid.addRow(name, edit, remove);
            }
            list.add(new WHorizontalSeparator());
        }

        // Add
        WPlus add = list.add(new WPlus());
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
    public void resize(MinecraftClient client, int width, int height) {
        list.maxHeight = height - 32;
        super.resize(client, width, height);
    }

    @Override
    public void onClose() {
        MeteorClient.eventBus.unsubscribe(this);
        super.onClose();
    }
}
