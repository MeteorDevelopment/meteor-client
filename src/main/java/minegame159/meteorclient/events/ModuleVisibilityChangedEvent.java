package minegame159.meteorclient.events;

import minegame159.jes.Event;
import minegame159.meteorclient.modules.Module;

public class ModuleVisibilityChangedEvent extends Event {
    public Module module;

    @Override
    public boolean isCancellable() {
        return false;
    }
}
