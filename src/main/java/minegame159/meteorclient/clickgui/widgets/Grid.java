package minegame159.meteorclient.clickgui.widgets;

import java.util.ArrayList;
import java.util.List;

public class Grid extends Widget {
    private int columns;
    private double horizontalSpacing, verticalSpacing;
    private List<Widget[]> rows = new ArrayList<>();
    private double[] columnWidths;
    private List<Double> rowHeights = new ArrayList<>();

    public Grid(double margin, int columns, double horizontalSpacing, double verticalSpacing) {
        super(0, 0, margin);
        this.columns = columns;
        this.horizontalSpacing = horizontalSpacing;
        this.verticalSpacing = verticalSpacing;
        columnWidths = new double[columns];
    }

    public void addRow(Widget... widgets) {
        if (widgets.length != columns) return;

        rows.add(widgets);
        addWidgets(widgets);
        layout();
    }

    @Override
    public void layout() {
        // Reset things
        for (int i = 0; i < columnWidths.length; i++) columnWidths[i] = 0;
        rowHeights.clear();

        // Calculate things
        for (int i = 0; i < rows.size(); i++) {
            Widget[] row = rows.get(i);
            rowHeights.add(0.0);

            for (int ii = 0; ii < row.length; ii++) {
                row[ii].layout();

                columnWidths[ii] = Math.max(columnWidths[ii], row[ii].widthMargin());
                rowHeights.set(i, Math.max(rowHeights.get(i), row[ii].heightMargin()));
            }
        }

        // Set things
        double y = this.y + margin;
        for (int i = 0; i < rows.size(); i++) {
            Widget[] row = rows.get(i);
            double x = this.x + margin;

            for (int ii = 0; ii < row.length; ii++) {
                row[ii].x = x;
                row[ii].y = y + (rowHeights.get(i) - row[ii].heightMargin()) / 2;
                row[ii].layout();

                x += columnWidths[ii] + horizontalSpacing;
            }

            y += rowHeights.get(i) + verticalSpacing;
        }

        // Calculate total size of the grid
        calculateSize();
    }
}
