/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.pathing;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public interface IPathManager {
    boolean isPathing();

    void pause();
    void resume();
    void stop();

    default void moveTo(BlockPos pos) { moveTo(pos, false); }
    void moveTo(BlockPos pos, boolean ignoreY);
    void moveInDirection(float yaw);

    void mine(Block... blocks);

    void follow(Predicate<Entity> entity);

    float getTargetYaw();
    float getTargetPitch();

    ISettings getSettings();

    interface ISettings {
        boolean getWalkOnWater();
        void setWalkOnWater(boolean value);

        boolean getWalkOnLava();
        void setWalkOnLava(boolean value);

        boolean getStep();
        void setStep(boolean value);

        boolean getNoFall();
        void setNoFall(boolean value);
    }
}
