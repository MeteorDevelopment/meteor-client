package minegame159.meteorclient.events.render;

import minegame159.meteorclient.utils.misc.Pool;
import net.minecraft.block.BlockState;

public class DrawSideEvent {  // TODO: Xray: async DrawSideEvent
    private static final Pool<DrawSideEvent> INSTANCE = new Pool<>(DrawSideEvent::new);

    public BlockState state;

    private boolean set, draw;

    private void reset() {
        set = draw = false;
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
        DrawSideEvent event = INSTANCE.get();
        event.reset();
        event.state = state;
        return event;
    }

    public static void returnDrawSideEvent(DrawSideEvent event) {
        INSTANCE.free(event);
    }
}
