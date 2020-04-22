package minegame159.meteorclient.gui.widgets;

public class WContainer extends WWidget {
    @Override
    protected void onCalculateSize() {
        double maxX = 0;
        double maxY = 0;

        for (Cell cell : getCells()) {
            // Set cell's position and size
            cell.x = x;
            cell.y = y;

            // Update maxX and maxY
            maxX = Math.max(maxX, cell.x);
            maxY = Math.max(maxY, cell.y);
        }

        // Calculate width and height
        width = maxX - x;
        height = maxY - y;
    }
}
