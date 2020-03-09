package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.ActiveModulesChangedEvent;
import minegame159.meteorclient.events.ModuleVisibilityChangedEvent;
import minegame159.meteorclient.events.Render2DEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class ActiveModules extends Module {
    private List<Module> modules = new ArrayList<>();
    private int infoColor = Color.fromRGBA(175, 175, 175, 255);

    public ActiveModules() {
        super(Category.Render, "active-modules", "Displays active modules.", false);
    }

    @Override
    public void onActivate() {
        recalculate();
    }

    private void recalculate() {
        modules.clear();

        for (Module module : ModuleManager.INSTANCE.getActive()) {
            if (module.isVisible()) modules.add(module);
        }

        modules.sort((o1, o2) -> {
            int a = Integer.compare(o1.getInfoString() == null ? Utils.getTextWidth(o1.title) : Utils.getTextWidth(o1.title + " " + o1.getInfoString()), o2.getInfoString() == null ? Utils.getTextWidth(o2.title) : Utils.getTextWidth(o2.title + " " + o2.getInfoString()));
            if (a == 0) return 0;
            return a < 0 ? 1 : -1;
        });
    }

    @EventHandler
    private Listener<ActiveModulesChangedEvent> activeModulesChangedEventListener = new Listener<>(event -> recalculate());

    @EventHandler
    private Listener<ModuleVisibilityChangedEvent> onModuleVisibilityChanged = new Listener<>(event -> {
        if (event.module.isActive()) recalculate();
    });

    @EventHandler
    private Listener<Render2DEvent> onRender2D = new Listener<>(event -> {
        if (mc.options.debugEnabled) return;
        int y = 2;

        for (Module module : modules) {
            String infoString = module.getInfoString();
            if (infoString == null) {
                int x = event.screenWidth - Utils.getTextWidth(module.title) - 2;
                Utils.drawTextWithShadow(module.title, x, y, module.color);
                y += Utils.getTextHeight() + 1;
            } else {
                int x = event.screenWidth - Utils.getTextWidth(module.title + " " + infoString) - 2;
                Utils.drawTextWithShadow(module.title, x, y, module.color);
                Utils.drawTextWithShadow(module.getInfoString(), x + Utils.getTextWidth(module.title + " "), y, infoColor);
                y += Utils.getTextHeight() + 1;
            }
        }
    });
}
