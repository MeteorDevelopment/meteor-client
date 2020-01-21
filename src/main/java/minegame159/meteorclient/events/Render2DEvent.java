package minegame159.meteorclient.events;

import minegame159.jes.Event;

public class Render2DEvent extends Event {
    public int screenWidth, screenHeight;
    public float tickDelta;

    @Override
    public boolean isCancellable() {
        return false;
    }
}
