package minegame159.meteorclient.events;

import minegame159.jes.Event;
import net.minecraft.entity.LivingEntity;

public class TookDamageEvent extends Event {
    public LivingEntity entity;

    @Override
    public boolean isCancellable() {
        return false;
    }
}
