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
        .description("Whether or not using items will slow you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<WebMode> web = sgGeneral.add(new EnumSetting.Builder<WebMode>()
        .name("web")
        .description("Whether or not cobwebs will not slow you down.")
        .defaultValue(WebMode.Vanilla)
        .build()
    );

    private final Setting<Double> webTimer = sgGeneral.add(new DoubleSetting.Builder()
        .name("web-timer")
        .description("The timer value for WebMode Timer.")
        .defaultValue(10)
        .min(1)
        .sliderMin(1)
        .visible(() -> web.get() == WebMode.Timer)
        .build()
    );

    private final Setting<Boolean> honeyBlock = sgGeneral.add(new BoolSetting.Builder()
        .name("honey-block")
        .description("Whether or not honey blocks will not slow you down.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> soulSand = sgGeneral.add(new BoolSetting.Builder()
        .name("soul-sand")
        .description("Whether or not soul sand will not slow you down.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> slimeBlock = sgGeneral.add(new BoolSetting.Builder()
        .name("slime-block")
        .description("Whether or not slime blocks will not slow you down.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> berryBush = sgGeneral.add(new BoolSetting.Builder()
        .name("berry-bush")
        .description("Whether or not berry bushes will not slow you down.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> airStrict = sgGeneral.add(new BoolSetting.Builder()
        .name("air-strict")
        .description("Will attempt to bypass anti-cheats like 2b2t's. Only works while in air.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> sneaking = sgGeneral.add(new BoolSetting.Builder()
        .name("sneaking")
        .description("Whether or not sneaking will not slow you down.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> hunger = sgGeneral.add(new BoolSetting.Builder()
        .name("hunger")
        .description("Whether or not hunger will not slow you down.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> slowness = sgGeneral.add(new BoolSetting.Builder()
        .name("slowness")
        .description("Whether or not slowness will not slow you down.")
        .defaultValue(false)
        .build()
    );

    private boolean resetTimer;

    public NoSlow() {
        super(Categories.Movement, "no-slow", "Allows you to move normally when using objects that will slow you.");
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
