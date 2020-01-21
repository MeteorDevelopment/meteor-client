package minegame159.meteorclient.events;

import minegame159.jes.Event;

public class TickEvent extends Event {
    @Override
    public boolean isCancellable() {
        return false;
    }
}
