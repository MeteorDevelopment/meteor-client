package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.Alignment;
import minegame159.meteorclient.gui.WidgetLayout;
import minegame159.meteorclient.utils.Box;
import minegame159.meteorclient.utils.Vector2;

import java.util.ArrayList;
import java.util.List;

public class WGrid extends WWidget {
    private int columnCount;
    private Alignment.X defaultAlignmentX;
    private Alignment.Y defaultAlignmentY;

    private List<WWidget[]> rows = new ArrayList<>();

    public WGrid(double horizontalSpacing, double verticalSpacing, int columnCount, Alignment.X defaultAlignmentX, Alignment.Y defaultAlignmentY) {
        boundingBox.autoSize = true;
        layout = new GridLayout(horizontalSpacing, verticalSpacing);

        this.columnCount = columnCount;
        this.defaultAlignmentX = defaultAlignmentX;
        this.defaultAlignmentY = defaultAlignmentY;
    }
    public WGrid(double horizontalSpacing, double verticalSpacing, int columnCount) {
        this(horizontalSpacing, verticalSpacing, columnCount, Alignment.X.Left, Alignment.Y.Center);
    }

    public void addRow(WWidget... row) {
        if (row.length != columnCount) return;

        for (WWidget widget : row) {
            widget.boundingBox.alignment.set(defaultAlignmentX, defaultAlignmentY);
            add(widget);
        }
        rows.add(row);
    }

    public void removeRow(int i) {
        WWidget[] toRemove = rows.remove(i);
        for (WWidget widget : toRemove) {
            widgets.remove(widget);
        }
    }
    public void removeLastRow() {
        removeRow(rows.size() - 1);
    }

    @Override
    public void calculatePosition() {
        layout.reset(this);

        for (int i = 0; i < rows.size(); i++) {
            WWidget[] row = rows.get(i);

            for (int ii = 0; ii < row.length; ii++) {
                WWidget widget = row[ii];

                if (widget.boundingBox.calculateAutoSizePost) widget.boundingBox.calculateCustomSize();

                Box box = ((GridLayout) layout).layoutWidget(this, i, ii);
                widget.boundingBox.calculatePos(box);

                widget.calculatePosition();
            }
        }
    }

    private static class GridLayout extends WidgetLayout {
        private double horizontalSpacing;
        private double verticalSpacing;

        private List<Double> columnWidths = new ArrayList<>();
        private List<Double> rowHeights = new ArrayList<>();

        private Box box = new Box();

        public GridLayout(double horizontalSpacing, double verticalSpacing) {
            this.horizontalSpacing = horizontalSpacing;
            this.verticalSpacing = verticalSpacing;
        }

        @Override
        public void reset(WWidget widget) {
        }

        @Override
        public Vector2 calculateAutoSize(WWidget widget) {
            WGrid grid = (WGrid) widget;

            columnWidths.clear();
            rowHeights.clear();

            for (WWidget[] row : grid.rows) {
                // Calculate column widths
                for (int i = 0; i < row.length; i++) {
                    if (columnWidths.size() >= i) columnWidths.add(0.0);
                    columnWidths.set(i, Math.max(columnWidths.get(i), row[i].boundingBox.getWidth()));
                }

                // Calculate row heights
                double rowHeight = 0;
                for (WWidget w : row) rowHeight = Math.max(rowHeight, w.boundingBox.getHeight());
                rowHeights.add(rowHeight);
            }

            Vector2 size = new Vector2();

            int i = 0;
            for (WWidget[] row : grid.rows) {
                // Width
                if (i == 0) {
                    for (int ii = 0; ii < row.length; ii++) {
                        if (ii > 0) size.x += horizontalSpacing;
                        size.x += columnWidths.get(ii);
                    }
                }

                // Height
                if (i > 0) size.y += verticalSpacing;
                size.y += rowHeights.get(i);

                i++;
            }

            return size;
        }

        public Box layoutWidget(WWidget widget, int rowI, int columnI) {
            box.x = widget.boundingBox.getInnerX();
            box.y = widget.boundingBox.getInnerY();

            // X
            int i;
            for (i = 0; i < columnI; i++) {
                if (i > 0) box.x += horizontalSpacing;
                box.x += columnWidths.get(i);
            }
            if (i > 0) box.x += horizontalSpacing;

            // Y
            for (i = 0; i < rowI; i++) {
                if (i > 0) box.y += verticalSpacing;
                box.y += rowHeights.get(i);
            }
            if (i > 0) box.y += verticalSpacing;

            box.width = columnWidths.get(columnI);
            box.height = rowHeights.get(rowI);

            return box;
        }

        @Override
        public Box layoutWidget(WWidget widget, WWidget child) {
            return null;
        }
    }
}
