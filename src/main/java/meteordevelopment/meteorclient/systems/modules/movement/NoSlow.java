/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;

public class NoSlow extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> items = sgGeneral.add(new BoolSetting.Builder()
        .name("items")
        .defaultValue(true)
        .build()
    );

    private final Setting<WebMode> web = sgGeneral.add(new EnumSetting.Builder<WebMode>()
        .name("web")
        .defaultValue(WebMode.Vanilla)
        .build()
    );

    private final Setting<Double> webTimer = sgGeneral.add(new DoubleSetting.Builder()
        .name("web-timer")
        .defaultValue(10)
        .min(1)
        .sliderMin(1)
        .visible(() -> web.get() == WebMode.Timer)
        .build()
    );

    private final Setting<Boolean> honeyBlock = sgGeneral.add(new BoolSetting.Builder()
        .name("honey-block")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> soulSand = sgGeneral.add(new BoolSetting.Builder()
        .name("soul-sand")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> slimeBlock = sgGeneral.add(new BoolSetting.Builder()
        .name("slime-block")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> berryBush = sgGeneral.add(new BoolSetting.Builder()
        .name("berry-bush")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> airStrict = sgGeneral.add(new BoolSetting.Builder()
        .name("air-strict")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> fluidDrag = sgGeneral.add(new BoolSetting.Builder()
        .name("fluid-drag")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> sneaking = sgGeneral.add(new BoolSetting.Builder()
        .name("sneaking")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> hunger = sgGeneral.add(new BoolSetting.Builder()
        .name("hunger")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> slowness = sgGeneral.add(new BoolSetting.Builder()
        .name("slowness")
        .defaultValue(false)
        .build()
    );

    private boolean resetTimer;

    public NoSlow() {
        super(Categories.Movement, "no-slow");
    }

    @Override
    public void onActivate() {
        resetTimer = false;
    }

    public boolean airStrict() {
        return isActive() && airStrict.get() && mc.player.isUsingItem();
    }

    public boolean items() {
        return isActive() && items.get();
    }

    public boolean honeyBlock() {
        return isActive() && honeyBlock.get();
    }

    public boolean soulSand() {
        return isActive() && soulSand.get();
    }

    public boolean slimeBlock() {
        return isActive() && slimeBlock.get();
    }

    public boolean cobweb() {
        return isActive() && web.get() == WebMode.Vanilla;
    }

    public boolean berryBush() {
        return isActive() && berryBush.get();
    }

    public boolean fluidDrag() {
        return isActive() && fluidDrag.get();
    }

    public boolean sneaking() {
        return isActive() && sneaking.get();
    }

    public boolean hunger() {
        return isActive() && hunger.get();
    }

    public boolean slowness() {
        return isActive() && slowness.get();
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (web.get() == WebMode.Timer) {
            if (mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.COBWEB && !mc.player.isOnGround()) {
                resetTimer = false;
                Modules.get().get(Timer.class).setOverride(webTimer.get());
            } else if (!resetTimer) {
                Modules.get().get(Timer.class).setOverride(Timer.OFF);
                resetTimer = true;
            }
        }
    }

    public enum WebMode {
        Vanilla,
        Timer,
        None
    }
}
