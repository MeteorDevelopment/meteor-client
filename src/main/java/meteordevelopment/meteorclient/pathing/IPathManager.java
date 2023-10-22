/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.pathing;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.Settings;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public interface IPathManager {
    String getName();

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
        Settings get();

        Setting<Boolean> getWalkOnWater();
        Setting<Boolean> getWalkOnLava();

        Setting<Boolean> getStep();
        Setting<Boolean> getNoFall();

        void save();
    }
}
