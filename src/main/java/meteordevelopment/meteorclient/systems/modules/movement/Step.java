/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import com.google.common.collect.Streams;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.OptionalDouble;

public class Step extends Module {
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
        .range(1, 36)
        .sliderRange(1, 36)
        .visible(safeStep::get)
        .build()
    );

    private float prevStepHeight;
    private boolean prevPathManagerStep;

    public Step() {
        super(Categories.Movement, "step", "Allows you to walk up full blocks instantly.");
    }

    @Override
    public void onActivate() {
        prevStepHeight = mc.player.getStepHeight();

        prevPathManagerStep = PathManagers.get().getSettings().getStep().get();
        PathManagers.get().getSettings().getStep().set(true);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        boolean work = (activeWhen.get() == ActiveWhen.Always) || (activeWhen.get() == ActiveWhen.Sneaking && mc.player.isSneaking()) || (activeWhen.get() == ActiveWhen.NotSneaking && !mc.player.isSneaking());
        double height = getMaxSafeHeight();
        if (work && height > 0) {
            mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT).setBaseValue(height);
        } else {
            mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT).setBaseValue(prevStepHeight);
        }
    }

    @Override
    public void onDeactivate() {
        mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT).setBaseValue(prevStepHeight);

        PathManagers.get().getSettings().getStep().set(prevPathManagerStep);
    }

    private float getHealth() {
        return mc.player.getHealth() + mc.player.getAbsorptionAmount();
    }

    private double getExplosionDamage() {
        OptionalDouble crystalDamage = Streams.stream(mc.world.getEntities())
                .filter(entity -> entity instanceof EndCrystalEntity)
                .filter(Entity::isAlive)
                .mapToDouble(entity -> DamageUtils.crystalDamage(mc.player, entity.getEntityPos()))
                .max();
        return crystalDamage.orElse(0.0);
    }

    private boolean isSafe() {
        return getHealth() > stepHealth.get() && getHealth() - getExplosionDamage() > stepHealth.get();
    }

    private boolean isSaferThanWith(double damage) {
        return isSafe() || getExplosionDamage() - damage <= 0;
    }

    private double getMaxSafeHeight() {
        if (!safeStep.get()) return height.get();

        double max = height.get();
        double h = 0;
        double currentDamage =getExplosionDamage();
        Box initial = mc.player.getBoundingBox();

        // all of this is to avoid running into crystals which are behind
        // one block when holding a movement key because standing on the
        // near edge of that block is technically safe

        Vec3d inputOffset = mc.player.getRotationVector();
        Vec2f input = mc.player.input.getMovementInput();
        ((IVec3d) inputOffset).meteor$setY(0);
        inputOffset = inputOffset.normalize().multiply(1.2);
        double zdot = inputOffset.z;
        double xdot = inputOffset.x;
        inputOffset = new Vec3d(input.y * xdot + input.x * zdot, 0, input.x * xdot + input.y * zdot);

        for (int i = 1; i < max; i++) {
            mc.player.setBoundingBox(initial.offset(0, i, 0));
            if (!isSaferThanWith(currentDamage)) {
                mc.player.setBoundingBox(initial);
                return h;
            }

            mc.player.setBoundingBox(mc.player.getBoundingBox().offset(inputOffset));
            if (!isSaferThanWith(currentDamage)) {
                mc.player.setBoundingBox(initial);
                return h;
            }
            h += 1;
        }
        mc.player.setBoundingBox(initial.offset(0, max, 0));

        if (isSaferThanWith(currentDamage)) h = max;

        mc.player.setBoundingBox(initial);
        return h;
    }

    public enum ActiveWhen {
        Always,
        Sneaking,
        NotSneaking
    }
}
