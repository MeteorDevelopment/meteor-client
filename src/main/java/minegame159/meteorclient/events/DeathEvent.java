package minegame159.meteorclient.events;

import minegame159.jes.Event;

public class DeathEvent extends Event {
    @Override
    public boolean isCancellable() {
        return false;
    }
}
