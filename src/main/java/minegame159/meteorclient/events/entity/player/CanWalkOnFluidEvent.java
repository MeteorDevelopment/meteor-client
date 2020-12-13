package minegame159.meteorclient.events.entity.player;

import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;

public class CanWalkOnFluidEvent {
    public LivingEntity entity;
    public Fluid fluid;

    public boolean walkOnFluid;
}
