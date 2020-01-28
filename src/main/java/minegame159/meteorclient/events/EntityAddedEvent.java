package minegame159.meteorclient.events;

import minegame159.jes.Event;
import net.minecraft.entity.Entity;

public class EntityAddedEvent extends Event {
    public Entity entity;

    @Override
    public boolean isCancellable() {
        return false;
    }
}
