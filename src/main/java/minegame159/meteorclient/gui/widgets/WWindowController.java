package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.gui.GuiConfig;
import net.minecraft.client.MinecraftClient;

public class WWindowController extends WWidget {
    public WWindowController() {
        for (Category category : ModuleManager.CATEGORIES) {
            add(new WModuleGroup(category));
        }

        add(new WProfiles());
        add(new WSearch());
    }

    @Override
    protected void onCalculateSize() {
        width = parent != null ? parent.width - (parent.width - x) : 0;
        height = parent != null ? parent.height - (parent.height - y) : 0;
    }

    @Override
    protected void onCalculateWidgetPositions() {
        double cellX = x;
        double cellY = y + 16;

        for (Cell<?> cell : cells) {
            GuiConfig.WindowConfig winConfig = GuiConfig.INSTANCE.getWindowConfig(((WWindow) cell.getWidget()).type, false);
            boolean automatic = false;

            if (winConfig.getX() != -1 && winConfig.getY() != -1) {
                cellX = winConfig.getX();
                cellY = winConfig.getY();
            } else {
                cellX += 16;
                automatic = true;
            }

            cell.width = ((WWindow) cell.getWidget()).width;
            cell.height = ((WWindow) cell.getWidget()).height;

            if (automatic) {
                if (cellX + cell.width > MinecraftClient.getInstance().window.getScaledWidth()) {
                    cellX = x;
                    cellY += 10 + cell.height + 10;
                }
            }

            cell.x = cellX;
            cell.y = cellY;

            cellX += cell.width;

            cell.alignWidget();
        }
    }
}
