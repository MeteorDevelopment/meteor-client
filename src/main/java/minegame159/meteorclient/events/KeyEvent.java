package minegame159.meteorclient.events;

import minegame159.jes.Event;

public class KeyEvent extends Event {
    public int key;
    public boolean push;

    @Override
    public boolean isCancellable() {
        return false;
    }
}
