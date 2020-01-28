package minegame159.meteorclient.events;

import minegame159.jes.Event;

public class CharTypedEvent extends Event {
    public char c;

    @Override
    public boolean isCancellable() {
        return true;
    }
}
