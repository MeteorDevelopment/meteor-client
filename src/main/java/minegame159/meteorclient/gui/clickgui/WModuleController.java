package minegame159.meteorclient.gui.clickgui;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.WidgetLayout;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.Box;
import minegame159.meteorclient.utils.Vector2;

public class WModuleController extends WWidget {
    public WModuleController() {
        layout = new ModuleControllerLayout();
        boundingBox.setMargin(16);

        for (Category category : ModuleManager.CATEGORIES) {
            add(new WModuleGroup(category));
        }

        add(new WProfiles());
    }

    private static class ModuleControllerLayout extends WidgetLayout {
        private Box box = new Box();

        @Override
        public void reset(WWidget widget) {

        }

        @Override
        public Vector2 calculateAutoSize(WWidget widget) {
            return null;
        }

        @Override
        public Box layoutWidget(WWidget widget, WWidget child) {
            Vector2 pos = null;
            if (child instanceof WModuleGroup) pos = Config.INSTANCE.getGuiPosition(((WModuleGroup) child).category);
            else if (child instanceof WProfiles) pos = Config.INSTANCE.getGuiPosition(Category.Profiles);

            if (pos != null) {
                box.x = pos.x;
                box.y = pos.y;
            } else box.x += 10 + box.width;

            box.width = child.boundingBox.getWidth();
            box.height = child.boundingBox.getHeight();

            return box;
        }
    }
}
