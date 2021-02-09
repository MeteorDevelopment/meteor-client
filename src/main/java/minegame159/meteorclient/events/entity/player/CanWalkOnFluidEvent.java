package minegame159.meteorclient.events.entity.player;

import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;

public class CanWalkOnFluidEvent {

    private static final CanWalkOnFluidEvent INSTANCE = new CanWalkOnFluidEvent();

    public LivingEntity entity;
    public Fluid fluid;
    public boolean walkOnFluid;

    public static CanWalkOnFluidEvent get(LivingEntity entity, Fluid fluid) {
        INSTANCE.entity = entity;
        INSTANCE.fluid = fluid;
        INSTANCE.walkOnFluid = false;
        return INSTANCE;
    }
}
