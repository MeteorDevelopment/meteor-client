/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.pathing;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public class NopPathManager implements IPathManager {
    private final Settings settings = new Settings();

    @Override
    public boolean isPathing() {
        return false;
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void stop() {}

    @Override
    public void moveTo(BlockPos pos, boolean ignoreY) {}

    @Override
    public void moveInDirection(float yaw) {}

    @Override
    public void mine(Block... blocks) {}

    @Override
    public void follow(Predicate<Entity> entity) {}

    @Override
    public float getTargetYaw() {
        return 0;
    }

    @Override
    public float getTargetPitch() {
        return 0;
    }

    @Override
    public ISettings getSettings() {
        return settings;
    }

    private static class Settings implements ISettings {
        @Override
        public boolean getWalkOnWater() {
            return false;
        }

        @Override
        public void setWalkOnWater(boolean value) {}

        @Override
        public boolean getWalkOnLava() {
            return false;
        }

        @Override
        public void setWalkOnLava(boolean value) {}

        @Override
        public boolean getStep() {
            return false;
        }

        @Override
        public void setStep(boolean value) {}

        @Override
        public boolean getNoFall() {
            return false;
        }

        @Override
        public void setNoFall(boolean value) {}
    }
}
