package minegame159.meteorclient.events.render;

import net.minecraft.block.BlockState;

public class DrawSideEvent {

    private static final DrawSideEvent INSTANCE = new DrawSideEvent();

    public BlockState state;

    private boolean set, draw;

    public void reset() {
        set = false;
    }

    public void setDraw(boolean draw) {
        set = true;
        this.draw = draw;
    }

    public boolean isSet() {
        return set;
    }
    public boolean getDraw() {
        return draw;
    }

    public static DrawSideEvent get(BlockState state) {
        INSTANCE.reset();
        INSTANCE.state = state;
        return INSTANCE;
    }
}
