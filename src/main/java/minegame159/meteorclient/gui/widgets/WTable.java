/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import minegame159.meteorclient.gui.renderer.GuiRenderer;

import java.util.ArrayList;
import java.util.List;

public class WTable extends WWidget {
    public final Cell<?> defaultCell = new Cell<>().centerY().space(4);

    private double paddingVertical, paddingHorizontal;

    private final List<List<Cell<?>>> rows = new ArrayList<>(1);
    private int rowI;

    private final DoubleList rowWidths = new DoubleArrayList(1);
    private final DoubleList rowHeights = new DoubleArrayList(1);
    private final DoubleList columnWidths = new DoubleArrayList(1);

    private final IntList rowFillXCount = new IntArrayList(1);

    private final DoubleList rowSpaceTop = new DoubleArrayList(1);
    private final DoubleList rowSpaceBottom = new DoubleArrayList(1);

    @Override
    public <T extends WWidget> Cell<T> add(T widget) {
        Cell<T> cell = super.add(widget);
        cell.set(defaultCell);
        List<Cell<?>> row;
        if (rows.size() <= rowI) {
            row = new ArrayList<>(1);
            rows.add(row);
        } else row = rows.get(rowI);
        row.add(cell);

        if (widget instanceof WHorizontalSeparator) {
            cell.fillX().expandX();
            if (parent instanceof WWindow) ((WWindow) parent).row();
            else row();
        } else if (widget instanceof WVerticalSeparator) {
            cell.expandY();
        } else if (widget instanceof WSection) {
            cell.fillX().expandX();
        }

        return cell;
    }

    public void row() {
        rowI++;
    }

    protected void removeRow() {
        rowI--;
    }

    @Override
    public void clear() {
        super.clear();
        rows.clear();
        rowI = 0;
    }

    public WTable pad(double pad) {
        paddingVertical = pad;
        paddingHorizontal = pad;
        return this;
    }

    public WTable padVertical(double pad) {
        paddingVertical = pad;
        return this;
    }

    public WTable padHorizontal(double pad) {
        paddingHorizontal = pad;
        return this;
    }

    public double getVerticalPad() {
        return paddingVertical;
    }

    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        calculateInfo();

        // Reset
        rowWidths.clear();

        double y = this.y + paddingVertical;
        double maxX = this.x + paddingHorizontal;
        double maxY = this.y + paddingVertical;
        double spaceVertical = 0;

        for (int rowI = 0; rowI < rows.size(); rowI++) {
            List<Cell<?>> row = rows.get(rowI);

            double x = this.x + paddingHorizontal;
            double rowMaxX = this.x + paddingHorizontal;
            double rowHeight = rowHeights.getDouble(rowI);
            double spaceHorizontal = 0;

            // Apply vertical spacing
            if (rowI > 0) {
                spaceVertical = Math.max(spaceVertical, rowSpaceTop.getDouble(rowI));
                y += spaceVertical;
            }
            spaceVertical = rowSpaceBottom.getDouble(rowI);

            for (int cellI = 0; cellI < row.size(); cellI++) {
                Cell<?> cell = row.get(cellI);

                double columnWidth = columnWidths.getDouble(cellI);

                // Apply horizontal spacing
                if (cellI > 0) {
                    spaceHorizontal = Math.max(spaceHorizontal, cell.spaceLeft);
                    x += spaceHorizontal;
                }
                spaceHorizontal = cell.spaceRight;

                // Calculate cell's position and size
                cell.x = x + cell.padLeft;
                cell.y = y + cell.padTop;
                cell.width = columnWidth - cell.padLeft - cell.padRight;
                cell.height = rowHeight - cell.padTop - cell.padBottom;

                // Update maxX
                rowMaxX += columnWidth + (cellI > 0 ? spaceHorizontal : 0);

                // Update x
                x += columnWidth;
            }

            // Update row width
            rowWidths.add(rowMaxX - this.x + paddingHorizontal);

            // Update max x and y
            maxX = Math.max(maxX, rowMaxX);
            maxY += rowHeight + (rowI > 0 ? spaceVertical : 0);

            // Update y
            y += rowHeight;
        }

        // Calculate size
        width = maxX - this.x + paddingHorizontal;
        height = maxY - this.y + paddingVertical;
    }

    @Override
    protected void onCalculateWidgetPositions() {
        double y = this.y + paddingVertical;
        double spaceVertical = 0;

        for (int rowI = 0; rowI < rows.size(); rowI++) {
            List<Cell<?>> row = rows.get(rowI);

            double x = this.x + paddingHorizontal;
            double rowHeight = rowHeights.getDouble(rowI);
            double fillXAdd = rowFillXCount.getInt(rowI) > 0 ? (width - rowWidths.getDouble(rowI)) / rowFillXCount.getInt(rowI) : 0;

            // Apply vertical spacing
            if (rowI > 0) {
                spaceVertical = Math.max(spaceVertical, rowSpaceTop.getDouble(rowI));
                y += spaceVertical;
            }
            spaceVertical = rowSpaceBottom.getDouble(rowI);

            for (int cellI = 0; cellI < row.size(); cellI++) {
                Cell<?> cell = row.get(cellI);

                double columnWidth = columnWidths.getDouble(cellI);
                double spaceHorizontal = 0;

                // Apply horizontal spacing
                if (cellI > 0) {
                    spaceHorizontal = Math.max(spaceHorizontal, cell.spaceLeft);
                    x += spaceHorizontal;
                }
                spaceHorizontal = cell.spaceRight;

                // Calculate cell's position and apply fillX
                cell.x = x + cell.padLeft;
                cell.y = y + cell.padTop;
                cell.width += cell.fillX ? fillXAdd : 0;

                // Align widget to the cell
                cell.alignWidget();

                // Update x
                x += columnWidth + (cell.fillX ? fillXAdd : 0);
            }

            // Update y
            y += rowHeight;
        }
    }

    private void calculateInfo() {
        // Reset
        rowHeights.clear();
        columnWidths.clear();

        rowFillXCount.clear();

        rowSpaceTop.clear();
        rowSpaceBottom.clear();

        for (List<Cell<?>> row : rows) {
            double rowHeight = 0;
            int rowFillXCount = 0;
            double rowSpaceTop = 0;
            double rowSpaceBottom = 0;

            for (int cellI = 0; cellI < row.size(); cellI++) {
                Cell<?> cell = row.get(cellI);

                // Calculate row height
                rowHeight = Math.max(rowHeight, cell.padTop + cell.getWidget().height + cell.padBottom);

                // Calculate column width
                double cellWidth = cell.padLeft + cell.getWidget().width + cell.padRight;
                if (columnWidths.size() <= cellI) columnWidths.add(cellWidth);
                else columnWidths.set(cellI, Math.max(columnWidths.getDouble(cellI), cellWidth));

                // Calculate row fillX
                if (cell.fillX) rowFillXCount++;

                // Calculate row space top and bottom
                rowSpaceTop = Math.max(rowSpaceTop, cell.spaceTop);
                rowSpaceBottom = Math.max(rowSpaceBottom, cell.spaceBottom);
            }

            // Save calculated info
            rowHeights.add(rowHeight);

            this.rowFillXCount.add(rowFillXCount);

            this.rowSpaceTop.add(rowSpaceTop);
            this.rowSpaceBottom.add(rowSpaceBottom);
        }
    }
}
