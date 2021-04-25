/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.movement;

import baritone.api.BaritoneAPI;
import com.google.common.collect.Streams;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.player.DamageCalcUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;

import java.util.Comparator;
import java.util.Optional;

public class Step extends Module {
    public enum ActiveWhen {
        Always,
        Sneaking,
        NotSneaking
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    public final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
            .name("height")
            .description("Step height.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private final Setting<ActiveWhen> activeWhen = sgGeneral.add(new EnumSetting.Builder<ActiveWhen>()
            .name("active-when")
            .description("Step is active when you meet these requirements.")
            .defaultValue(ActiveWhen.Always)
            .build()
    );

    private final Setting<Boolean> safeStep = sgGeneral.add(new BoolSetting.Builder()
            .name("safe-step")
            .description("Doesn't let you step out of a hole if you are low on health or there is a crystal nearby.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> stepHealth = sgGeneral.add(new IntSetting.Builder()
            .name("step-health")
            .description("The health you stop being able to step at.")
            .defaultValue(5)
            .min(1)
            .max(36)
            .build()
    );

    private float prevStepHeight;
    private boolean prevBaritoneAssumeStep;

    public Step() {
        super(Categories.Movement, "step", "Allows you to walk up full blocks instantly.");
    }

    @Override
    public void onActivate() {
        assert mc.player != null;
        prevStepHeight = mc.player.stepHeight;

        prevBaritoneAssumeStep = BaritoneAPI.getSettings().assumeStep.value;
        BaritoneAPI.getSettings().assumeStep.value = true;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        boolean work = (activeWhen.get() == ActiveWhen.Always) || (activeWhen.get() == ActiveWhen.Sneaking && mc.player.isSneaking()) || (activeWhen.get() == ActiveWhen.NotSneaking && !mc.player.isSneaking());
        mc.player.setBoundingBox(mc.player.getBoundingBox().offset(0, 1, 0));
        if (work && (!safeStep.get() || (getHealth() > stepHealth.get() && getHealth() - getExplosionDamage() > stepHealth.get()))){
            mc.player.stepHeight = height.get().floatValue();
        } else {
            mc.player.stepHeight = prevStepHeight;
        }
        mc.player.setBoundingBox(mc.player.getBoundingBox().offset(0, -1, 0));
    }

    @Override
    public void onDeactivate() {
        if (mc.player != null) mc.player.stepHeight = prevStepHeight;

        BaritoneAPI.getSettings().assumeStep.value = prevBaritoneAssumeStep;
    }

    private float getHealth(){
        assert mc.player != null;
        return mc.player.getHealth() + mc.player.getAbsorptionAmount();
    }

    private double getExplosionDamage(){
        assert mc.player != null;
        assert mc.world != null;
        Optional<EndCrystalEntity> crystal = Streams.stream(mc.world.getEntities())
                .filter(entity -> entity instanceof EndCrystalEntity)
                .filter(Entity::isAlive)
                .max(Comparator.comparingDouble(o -> DamageCalcUtils.crystalDamage(mc.player, o.getPos())))
                .map(entity -> (EndCrystalEntity) entity);
        return crystal.map(endCrystalEntity -> DamageCalcUtils.crystalDamage(mc.player, endCrystalEntity.getPos())).orElse(0.0);
    }
}
