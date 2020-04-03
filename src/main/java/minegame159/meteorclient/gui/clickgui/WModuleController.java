package minegame159.meteorclient.gui.clickgui;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.WidgetLayout;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.gui.widgets.WWindow;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.Box;
import minegame159.meteorclient.utils.Vector2;
import net.minecraft.client.MinecraftClient;

public class WModuleController extends WWidget {
    public WModuleController() {
        layout = new ModuleControllerLayout();
        boundingBox.setMargin(16);

        for (Category category : ModuleManager.CATEGORIES) {
            add(new WModuleGroup(category));
        }

        add(new WProfiles());
        add(new WSearch());
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
            Config.WindowConfig winConfig = Config.INSTANCE.getWindowConfig(((WWindow) child).getType(), false);
            boolean automatic = false;

            if (winConfig.getX() != -1 || winConfig.getY() != -1) {
                box.x = winConfig.getX();
                box.y = winConfig.getY();
            } else {
                box.x += 10 + box.width;
                automatic = true;
            }

            box.width = child.boundingBox.getWidth();
            box.height = child.boundingBox.getHeight();

            if (automatic) {
                if (box.x + box.width > MinecraftClient.getInstance().getWindow().getScaledWidth()) {
                    box.x = 10;
                    box.y += 10 + box.height + 10;
                }
            }

            return box;
        }
    }
}
