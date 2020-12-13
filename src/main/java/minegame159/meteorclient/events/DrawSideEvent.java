package minegame159.meteorclient.events;

import net.minecraft.block.BlockState;

public class DrawSideEvent {
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
}
