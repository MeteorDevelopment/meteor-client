package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.Utils;

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
                cellX += 2;
                automatic = true;
            }

            cell.width = ((WWindow) cell.getWidget()).width;
            cell.height = ((WWindow) cell.getWidget()).height;

            if (automatic) {
                if (cellX + cell.width > Utils.getScaledWindowWidthGui()) {
                    cellX = x;
                    cellY += 10 + 40 + 10;
                }
            }

            if (cellX > Utils.getScaledWindowWidthGui()) {
                cellX = Utils.getScaledWindowWidthGui() / 2.0 - cell.width / 2.0;
                if (cellX < 0) cellX = 0;
            }
            if (cellY > Utils.getScaledWindowHeightGui()) {
                cellY = Utils.getScaledWindowHeightGui() / 2.0 - cell.height / 2.0;
                if (cellY < 0) cellY = 0;
            }

            if (cellX != winConfig.getX() || cellY != winConfig.getY()) {
                winConfig.setPos(cellX, cellY);
            }
            Config.INSTANCE.save();

            cell.x = cellX;
            cell.y = cellY;

            cellX += cell.width;

            cell.alignWidget();
        }
    }
}
