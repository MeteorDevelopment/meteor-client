package minegame159.meteorclient.events;

import minegame159.jes.Event;
import net.minecraft.client.gui.screen.Screen;

public class OpenScreenEvent extends Event {
    public Screen screen;

    @Override
    public boolean isCancellable() {
        return true;
    }
}
