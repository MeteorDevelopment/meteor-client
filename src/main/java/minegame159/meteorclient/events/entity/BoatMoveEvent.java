package minegame159.meteorclient.events.entity;

import net.minecraft.entity.vehicle.BoatEntity;

public class BoatMoveEvent {
    private static final BoatMoveEvent INSTANCE = new BoatMoveEvent();

    public BoatEntity boat;

    public static BoatMoveEvent get(BoatEntity entity) {
        INSTANCE.boat = entity;
        return INSTANCE;
    }
}
