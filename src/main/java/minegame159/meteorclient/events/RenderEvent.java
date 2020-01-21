package minegame159.meteorclient.events;

import minegame159.jes.Event;

public class RenderEvent extends Event {
    public float tickDelta;

    @Override
    public boolean isCancellable() {
        return false;
    }
}
