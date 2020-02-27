package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.Alignment;
import minegame159.meteorclient.gui.WidgetLayout;
import minegame159.meteorclient.utils.Box;
import minegame159.meteorclient.utils.Vector2;

public class WHorizontalList extends WWidget {
    private Alignment.X defaultAlignmentX;
    private Alignment.Y defaultAlignmentY;

    public WHorizontalList(double spacing, Alignment.X defaultAlignmentX, Alignment.Y defaultAlignmentY) {
        boundingBox.autoSize = true;
        layout = new HorizontalListLayout(spacing);

        this.defaultAlignmentX = defaultAlignmentX;
        this.defaultAlignmentY = defaultAlignmentY;
    }
    public WHorizontalList(double spacing) {
        this(spacing, Alignment.X.Left, Alignment.Y.Center);
    }

    @Override
    public <T extends WWidget> T add(T widget) {
        widget.boundingBox.alignment.set(defaultAlignmentX, defaultAlignmentY);
        return super.add(widget);
    }

    public static class HorizontalListLayout extends WidgetLayout {
        private double spacing;

        private int i;
        private double lastChildWidth;
        private Box box = new Box();

        public HorizontalListLayout(double spacing) {
            this.spacing = spacing;
        }

        @Override
        public void reset(WWidget widget) {
            i = 0;
            box.x = widget.boundingBox.getInnerX();
            box.y = widget.boundingBox.getInnerY();
        }

        @Override
        public Vector2 calculateAutoSize(WWidget widget) {
            Vector2 size = new Vector2();

            int i = 0;
            for (WWidget w : widget.widgets) {
                size.x += w.boundingBox.getWidth();
                size.y = Math.max(size.y, w.boundingBox.getHeight());
                if (i > 0) size.x += spacing;
                i++;
            }

            return size;
        }

        @Override
        public Box layoutWidget(WWidget widget, WWidget child) {
            if (i > 0) box.x += spacing + lastChildWidth;

            box.width = child.boundingBox.getWidth();
            box.height = widget.boundingBox.innerHeight;

            lastChildWidth = child.boundingBox.getWidth();
            i++;
            return box;
        }
    }
}
