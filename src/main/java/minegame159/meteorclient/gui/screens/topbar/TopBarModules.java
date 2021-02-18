/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.screens.topbar;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Modules;
import net.minecraft.client.MinecraftClient;

public class TopBarModules extends TopBarScreen {
    public TopBarModules() {
        super(TopBarType.Modules);

        addTopBar();
        initWidgets();
    }

    private void initWidgets() {
        add(new WWindowController());

        // Help text
        WTable bottomLeft = add(new WTable()).bottom().left().getWidget();
        bottomLeft.pad(4);
        bottomLeft.add(new WLabel("Left click - activate/deactivate module", true));
        bottomLeft.row();
        bottomLeft.add(new WLabel("Right click - open module settings", true));
    }

    private static class WWindowController extends WWidget {
        public WWindowController() {
            for (Category category : Modules.loopCategories()) {
                add(new WModuleCategory(category));
            }

            add(new WProfiles());
            add(new WModuleSearch());
        }

        @Override
        protected void onCalculateSize(GuiRenderer renderer) {
            width = parent != null ? parent.width - (parent.width - x) : 0;
            height = parent != null ? parent.height - (parent.height - y) : 0;
        }

        @Override
        protected void onCalculateWidgetPositions() {
            double cellX = x + 4;
            double cellY = y + 40;

            for (Cell<?> cell : getCells()) {
                cell.width = cell.getWidget().width;
                cell.height = cell.getWidget().height;

                double screenWidth = MinecraftClient.getInstance().getWindow().getFramebufferWidth();
                double screenHeight = MinecraftClient.getInstance().getWindow().getFramebufferHeight();

                GuiConfig.WindowConfig config = ((WWindow) cell.getWidget()).getWindowConfig();
                if (config.getX() != -1) {
                    cellX = config.getX();
                    cellY = config.getY();
                }

                if (cellX + cell.width > screenWidth) {
                    cellX = x + 4;
                    cellY += 40;
                }

                if (cellX > screenWidth) {
                    cellX = screenWidth / 2.0 - cell.width / 2.0;
                    if (cellX < 0) cellX = 0;
                }
                if (cellY > screenHeight) {
                    cellY = screenHeight / 2.0 - cell.height / 2.0;
                    if (cellY < 0) cellY = 0;
                }

                cell.x = cellX;
                cell.y = cellY;

                cellX += cell.width + 4;
                cell.alignWidget();
            }
        }
    }
}
