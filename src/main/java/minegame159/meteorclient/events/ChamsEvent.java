package minegame159.meteorclient.events;

import minegame159.jes.Event;
import net.minecraft.entity.LivingEntity;

public class ChamsEvent extends Event {
    public LivingEntity livingEntity;
    public boolean enabled;

    @Override
    public boolean isCancellable() {
        return false;
    }
}
