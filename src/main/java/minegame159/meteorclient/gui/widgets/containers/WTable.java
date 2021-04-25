/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets.containers;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import minegame159.meteorclient.gui.utils.Cell;
import minegame159.meteorclient.gui.widgets.WWidget;

import java.util.ArrayList;
import java.util.List;

public class WTable extends WContainer {
    public double spacing = 3;

    private final List<List<Cell<?>>> rows = new ArrayList<>();
    private int rowI;

    private final DoubleList rowHeights = new DoubleArrayList();
    private final DoubleList columnWidths = new DoubleArrayList();

    private final DoubleList rowWidths = new DoubleArrayList();
    private final IntList rowExpandCellXCounts = new IntArrayList();

    @Override
    public <T extends WWidget> Cell<T> add(T widget) {
        Cell<T> cell = super.add(widget);

        if (rows.size() <= rowI) {
            List<Cell<?>> row = new ArrayList<>();
            row.add(cell);
            rows.add(row);
        }
        else rows.get(rowI).add(cell);

        return cell;
    }

    public void row() {
        rowI++;
    }

    @Override
    public void clear() {
        super.clear();
        rows.clear();
        rowI = 0;
    }

    protected double spacing() {
        return theme.scale(spacing);
    }

    @Override
    protected void onCalculateSize() {
        calculateInfo();

        // Reset
        rowWidths.clear();

        width = 0;
        height = 0;

        // Loop over rows
        for (int rowI = 0; rowI < rows.size(); rowI++) {
            List<Cell<?>> row = rows.get(rowI);

            double rowWidth = 0;

            // Loop over cells in the row
            for (int cellI = 0; cellI < row.size(); cellI++) {
                // Calculate row width
                if (cellI > 0) rowWidth += spacing();
                rowWidth += columnWidths.getDouble(cellI);
            }

            // Store row width
            rowWidths.add(rowWidth);
            width = Math.max(width, rowWidth);

            // Calculate height
            if (rowI > 0) height += spacing();
            height += rowHeights.getDouble(rowI);
        }
    }

    @Override
    protected void onCalculateWidgetPositions() {
        double y = this.y;

        // Loop over rows
        for (int rowI = 0; rowI < rows.size(); rowI++) {
            List<Cell<?>> row = rows.get(rowI);

            if (rowI > 0) y += spacing();

            double x = this.x;
            double rowHeight = rowHeights.getDouble(rowI);

            double expandXAdd = rowExpandCellXCounts.getInt(rowI) > 0 ? (width - rowWidths.getDouble(rowI)) / rowExpandCellXCounts.getInt(rowI) : 0;

            // Loop over cells in the row
            for (int cellI = 0; cellI < row.size(); cellI++) {
                Cell<?> cell = row.get(cellI);

                if (cellI > 0) x += spacing();
                double columnWidth = columnWidths.getDouble(cellI);

                cell.x = x;
                cell.y = y;

                cell.width = columnWidth + (cell.expandCellX ? expandXAdd : 0);
                cell.height = rowHeight;

                cell.alignWidget();

                x += columnWidth + (cell.expandCellX ? expandXAdd : 0);
            }

            y += rowHeight;
        }
    }

    private void calculateInfo() {
        // Reset
        rowHeights.clear();
        columnWidths.clear();
        rowExpandCellXCounts.clear();

        // Loop over rows
        for (List<Cell<?>> row : rows) {
            double rowHeight = 0;
            int rowExpandXCount = 0;

            // Loop over cells in the row
            for (int i = 0; i < row.size(); i++) {
                Cell<?> cell = row.get(i);

                // Calculate row height
                rowHeight = Math.max(rowHeight, cell.padTop() + cell.widget().height + cell.padBottom());

                // Calculate column width
                double cellWidth = cell.padLeft() + cell.widget().width + cell.padRight();
                if (columnWidths.size() <= i) columnWidths.add(cellWidth);
                else columnWidths.set(i, Math.max(columnWidths.getDouble(i), cellWidth));

                // Calculate row expandX count
                if (cell.expandCellX) rowExpandXCount++;
            }

            // Store calculated info
            rowHeights.add(rowHeight);
            rowExpandCellXCounts.add(rowExpandXCount);
        }
    }
}
