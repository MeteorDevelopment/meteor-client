/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.containers;

import meteordevelopment.meteorclient.gui.utils.Cell;

public class WVerticalList extends WContainer {
    public double spacing = 3;

    protected double widthRemove;

    protected double spacing() {
        return theme.scale(spacing);
    }

    @Override
    protected void onCalculateSize() {
        width = 0;
        height = 0;

        for (int i = 0; i < cells.size(); i++) {
            Cell<?> cell = cells.get(i);

            if (i > 0) height += spacing();

            width = Math.max(width, cell.padLeft() + cell.widget().width + cell.padRight());
            height += cell.padTop() + cell.widget().height + cell.padBottom();
        }
    }

    @Override
    protected void onCalculateWidgetPositions() {
        double y = this.y;

        for (int i = 0; i < cells.size(); i++) {
            Cell<?> cell = cells.get(i);

            if (i > 0) y += spacing();
            y += cell.padTop();

            cell.x = x + cell.padLeft();
            cell.y = y;

            cell.width = width - widthRemove - cell.padLeft() - cell.padRight();
            cell.height = cell.widget().height;

            cell.alignWidget();

            y += cell.height + cell.padBottom();
        }
    }
}
