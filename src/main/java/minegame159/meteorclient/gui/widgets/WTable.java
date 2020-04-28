package minegame159.meteorclient.gui.widgets;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import minegame159.meteorclient.gui.GuiRenderer;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class WTable extends WWidget {
    public final Cell<?> defaultCell = new Cell<>().centerY().space(4);

    public double maxHeight;
    public double animationProgress = 1;

    private List<List<Cell<?>>> rows = new ArrayList<>(1);
    private int rowI;

    private double padTop, padRight, padBottom, padLeft;

    private DoubleList rowWidths = new DoubleArrayList(1);
    private DoubleList rowHeights = new DoubleArrayList(1);
    private DoubleList columnWidths = new DoubleArrayList(1);

    private IntList rowFillXCount = new IntArrayList(1);

    private DoubleList rowSpaceTop = new DoubleArrayList(1);
    private DoubleList rowSpaceBottom = new DoubleArrayList(1);

    private double fullHeight = -1;
    private double verticalScroll;

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

    public WTable padTop(double pad) {
        padTop = pad;
        return this;
    }
    public WTable padRight(double pad) {
        padRight = pad;
        return this;
    }
    public WTable padBottom(double pad) {
        padBottom = pad;
        return this;
    }
    public WTable padLeft(double pad) {
        padLeft = pad;
        return this;
    }

    public WTable padHorizontal(double pad) {
        padRight = padLeft = pad;
        return this;
    }
    public WTable padVertical(double pad) {
        padTop = padBottom = pad;
        return this;
    }
    public WTable pad(double pad) {
        padTop = padRight = padBottom = padLeft = pad;
        return this;
    }

    @Override
    protected boolean onMouseScrolled(double amount) {
        if (fullHeight != -1 && mouseOver) {
            double preVerticalScroll = verticalScroll;
            verticalScroll += amount * 12;

            if (verticalScroll > 0) verticalScroll = 0;
            else if (verticalScroll < -(fullHeight - height)) verticalScroll = -(fullHeight - height);

            moveWidgets(0, verticalScroll - preVerticalScroll);
            return true;
        }

        return false;
    }

    @Override
    protected void onCalculateSize() {
        calculateInfo();

        // Reset
        rowWidths.clear();

        double y = this.y + padTop;
        double maxX = this.x + padLeft;
        double maxY = this.y + padTop;
        double spaceVertical = 0;

        for (int rowI = 0; rowI < rows.size(); rowI++) {
            List<Cell<?>> row = rows.get(rowI);

            double x = this.x + padLeft;
            double rowMaxX = this.x + padLeft;
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
            rowWidths.add(rowMaxX - this.x + padRight);

            // Update max x and y
            maxX = Math.max(maxX, rowMaxX);
            maxY += rowHeight + (rowI > 0 ? spaceVertical : 0);

            // Update y
            y += rowHeight;
        }

        // Calculate size
        width = maxX - this.x + padRight;
        height = maxY - this.y + padBottom;

        // Check if vertical scrolling needs to be enabled
        if (maxHeight != 0 && height > maxHeight) {
            fullHeight = height;
            height = maxHeight;

            if (verticalScroll < -(fullHeight - height)) verticalScroll = -(fullHeight - height);

            moveWidgets(0, -verticalScroll);
        } else {
            fullHeight = -1;
            verticalScroll = 0;
        }
    }

    @Override
    protected void onCalculateWidgetPositions() {
        double y = this.y + padTop;
        double spaceVertical = 0;

        for (int rowI = 0; rowI < rows.size(); rowI++) {
            List<Cell<?>> row = rows.get(rowI);

            double x = this.x + padLeft;
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

    private void moveWidgets(double deltaX, double deltaY) {
        for (Cell<?> cell : getCells()) move(cell.getWidget(), deltaX, deltaY);
        mouseMoved(MinecraftClient.getInstance().mouse.getX() / MinecraftClient.getInstance().getWindow().getScaleFactor(), MinecraftClient.getInstance().mouse.getY() / MinecraftClient.getInstance().getWindow().getScaleFactor());
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

    @Override
    public void render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (fullHeight != -1 || (animationProgress != 0 && animationProgress != 1)) {
            renderer.startScissor(this, (height - padTop) * (1 - animationProgress), 0, 0, 0);
            renderer.startTextScissor(this, (height - padTop) * (1 - animationProgress), 0, 0, 0);
        }
        super.render(renderer, mouseX, mouseY, delta);
        if (fullHeight != -1 || (animationProgress != 0 && animationProgress != 1)) {
            renderer.endTextScissor();
            renderer.endScissor();
        }
    }
}
