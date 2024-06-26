/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.containers;

import meteordevelopment.meteorclient.gui.utils.Cell;

public class WHorizontalList extends WContainer {
    public double spacing = 3;

    protected double calculatedWidth;
    protected int fillXCount;

    protected double spacing() {
        return theme.scale(spacing);
    }

    @Override
    protected void onCalculateSize() {
        width = 0;
        height = 0;

        fillXCount = 0;

        for (int i = 0; i < cells.size(); i++) {
            Cell<?> cell = cells.get(i);

            if (i > 0) width += spacing();

            width += cell.padLeft() + cell.widget().width + cell.padRight();
            height = Math.max(height, cell.padTop() + cell.widget().height + cell.padBottom());

            if (cell.expandCellX) fillXCount++;
        }

        calculatedWidth = width;
    }

    @Override
    protected void onCalculateWidgetPositions() {
        double x = this.x;
        double fillXWidth = (width - calculatedWidth) / fillXCount;

        for (int i = 0; i < cells.size(); i++) {
            Cell<?> cell = cells.get(i);

            if (i > 0) x += spacing();
            x += cell.padLeft();

            cell.x = x;
            cell.y = y + cell.padTop();

            cell.width = cell.widget().width;
            cell.height = height - cell.padTop() - cell.padTop();

            if (cell.expandCellX) cell.width += fillXWidth;
            cell.alignWidget();

            x += cell.width + cell.padRight();
        }
    }
}
