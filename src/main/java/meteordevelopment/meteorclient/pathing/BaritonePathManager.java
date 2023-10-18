/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.pathing;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.Rotation;
import baritone.api.utils.SettingsUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BaritonePathManager implements IPathManager {
    private final Field rotationField;
    private final Settings settings;

    private GoalDirection directionGoal;
    private boolean pathingPaused;

    public BaritonePathManager() {
        // Subscribe to event bus
        MeteorClient.EVENT_BUS.subscribe(this);

        // Find rotation field
        Class<?> klass = BaritoneAPI.getProvider().getPrimaryBaritone().getLookBehavior().getClass();
        Field rotationField = null;

        for (Field field : klass.getDeclaredFields()) {
            if (field.getType() == Rotation.class) {
                field.setAccessible(true);

                rotationField = field;
                break;
            }
        }

        this.rotationField = rotationField;

        // Create settings
        settings = new Settings();

        // Baritone pathing control
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingControlManager().registerProcess(new MeteorBaritoneProcess());
    }

    @Override
    public boolean isPathing() {
        return BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing();
    }

    @Override
    public void pause() {
        pathingPaused = true;
    }

    @Override
    public void resume() {
        pathingPaused = false;
    }

    @Override
    public void stop() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
    }

    @Override
    public void moveTo(BlockPos pos, boolean ignoreY) {
        if (ignoreY) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(pos.getX(), pos.getZ()));
            return;
        }

        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalGetToBlock(pos));
    }

    @Override
    public void moveInDirection(float yaw) {
        directionGoal = new GoalDirection(yaw);
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(directionGoal);
    }

    @Override
    public void mine(Block... blocks) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mine(blocks);
    }

    @Override
    public void follow(Predicate<Entity> entity) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getFollowProcess().follow(entity);
    }

    @Override
    public float getTargetYaw() {
        if (rotationField == null) return 0;

        try {
            return ((Rotation) rotationField.get(BaritoneAPI.getProvider().getPrimaryBaritone().getLookBehavior())).getYaw();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public float getTargetPitch() {
        if (rotationField == null) return 0;

        try {
            return ((Rotation) rotationField.get(BaritoneAPI.getProvider().getPrimaryBaritone().getLookBehavior())).getPitch();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ISettings getSettings() {
        return settings;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        if (directionGoal == null) return;

        if (directionGoal != BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().getGoal()) {
            directionGoal = null;
            return;
        }

        directionGoal.tick();
    }

    private static class Settings implements ISettings {
        @Override
        public boolean getWalkOnWater() {
            return BaritoneAPI.getSettings().assumeWalkOnWater.value;
        }

        @Override
        public void setWalkOnWater(boolean value) {
            BaritoneAPI.getSettings().assumeWalkOnWater.value = value;
        }

        @Override
        public boolean getWalkOnLava() {
            return BaritoneAPI.getSettings().assumeWalkOnLava.value;
        }

        @Override
        public void setWalkOnLava(boolean value) {
            BaritoneAPI.getSettings().assumeWalkOnLava.value = value;
        }

        @Override
        public boolean getStep() {
            return BaritoneAPI.getSettings().assumeStep.value;
        }

        @Override
        public void setStep(boolean value) {
            BaritoneAPI.getSettings().assumeStep.value = value;
        }

        @Override
        public boolean getNoFall() {
            return BaritoneAPI.getSettings().maxFallHeightNoWater.value > BaritoneAPI.getSettings().maxFallHeightNoWater.defaultValue;
        }

        @Override
        public void setNoFall(boolean value) {
            BaritoneAPI.getSettings().maxFallHeightNoWater.value = value ? 159159 : BaritoneAPI.getSettings().maxFallHeightNoWater.defaultValue;
        }
    }

    private static class GoalDirection implements Goal {
        private static final double SQRT_2 = Math.sqrt(2);

        private final float yaw;
        private int x;
        private int z;

        private int timer;

        public GoalDirection(float yaw) {
            this.yaw = yaw;
            tick();
        }

        public static double calculate(double xDiff, double zDiff) {
            double x = Math.abs(xDiff);
            double z = Math.abs(zDiff);
            double straight;
            double diagonal;
            if (x < z) {
                straight = z - x;
                diagonal = x;
            } else {
                straight = x - z;
                diagonal = z;
            }

            diagonal *= SQRT_2;
            return (diagonal + straight) * BaritoneAPI.getSettings().costHeuristic.value;
        }

        public void tick() {
            if (timer > 20) {
                timer = 0;

                Vec3d pos = mc.player.getPos();
                float theta = (float) Math.toRadians(yaw);

                x = (int) Math.floor(pos.x - (double) MathHelper.sin(theta) * 100);
                z = (int) Math.floor(pos.z + (double) MathHelper.cos(theta) * 100);
            }

            timer++;
        }

        public boolean isInGoal(int x, int y, int z) {
            return x == this.x && z == this.z;
        }

        public double heuristic(int x, int y, int z) {
            int xDiff = x - this.x;
            int zDiff = z - this.z;
            return calculate(xDiff, zDiff);
        }

        public String toString() {
            return String.format("GoalXZ{x=%s,z=%s}", SettingsUtil.maybeCensor(this.x), SettingsUtil.maybeCensor(this.z));
        }

        public int getX() {
            return this.x;
        }

        public int getZ() {
            return this.z;
        }
    }

    public class MeteorBaritoneProcess implements IBaritoneProcess {
        @Override
        public boolean isActive() {
            return pathingPaused;
        }

        @Override
        public PathingCommand onTick(boolean b, boolean b1) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().clearAllKeys();
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }

        @Override
        public boolean isTemporary() {
            return true;
        }

        @Override
        public void onLostControl() {
        }

        @Override
        public double priority() {
            return 0d;
        }

        @Override
        public String displayName0() {
            return "Meteor Client";
        }
    }
}
