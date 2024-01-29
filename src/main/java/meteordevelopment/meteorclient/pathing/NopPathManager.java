/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.pathing;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.Settings;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public class NopPathManager implements IPathManager {
    private final NopSettings settings = new NopSettings();

    @Override
    public String getName() {
        return "none";
    }

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

    private static class NopSettings implements ISettings {
        private final Settings settings = new Settings();
        private final Setting<Boolean> setting = new BoolSetting.Builder().build();

        @Override
        public Settings get() {
            return settings;
        }

        @Override
        public Setting<Boolean> getWalkOnWater() {
            setting.reset();
            return setting;
        }

        @Override
        public Setting<Boolean> getWalkOnLava() {
            setting.reset();
            return setting;
        }

        @Override
        public Setting<Boolean> getStep() {
            setting.reset();
            return setting;
        }

        @Override
        public Setting<Boolean> getNoFall() {
            setting.reset();
            return setting;
        }

        @Override
        public void save() {}
    }
}
