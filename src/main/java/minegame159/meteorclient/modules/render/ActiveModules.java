package minegame159.meteorclient.modules.render;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.ActiveModulesChangedEvent;
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
        super(Category.Render, "active-modules", "Displays active modules.");
    }

    @Override
    public void onActivate() {
        recalculate();
    }

    private void recalculate() {
        modules.clear();
        modules.addAll(ModuleManager.getActive());

        modules.sort((o1, o2) -> {
            int a = Integer.compare(o1.getInfoString() == null ? o1.title.length() : (o1.title + " " + o1.getInfoString()).length(), o2.getInfoString() == null ? o2.title.length() : (o2.title + " " + o2.getInfoString()).length());
            if (a == 0) return 0;
            return a < 0 ? 1 : -1;
        });
    }

    @SubscribeEvent
    private void onActiveModulesChanged(ActiveModulesChangedEvent e) {
        recalculate();
    }

    @SubscribeEvent
    private void onRender2D(Render2DEvent e) {
        int y = 2;

        for (Module module : modules) {
            String infoString = module.getInfoString();
            if (infoString == null) {
                int x = e.screenWidth - Utils.getTextWidth(module.title) - 2;
                Utils.drawText(module.title, x, y, module.color);
                y += Utils.getTextHeight() + 1;
            } else {
                int x = e.screenWidth - Utils.getTextWidth(module.title + " " + infoString) - 2;
                Utils.drawText(module.title, x, y, module.color);
                Utils.drawText(module.getInfoString(), x + Utils.getTextWidth(module.title + " "), y, infoColor);
                y += Utils.getTextHeight() + 1;
            }
        }
    }
}
