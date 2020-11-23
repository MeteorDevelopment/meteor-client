/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.KeyEvent;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.events.packets.PlaySoundPacketEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;

public class AutoFish extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSplashRangeDetection = settings.createGroup("Splash Sound Range Detection");

    // General
    private final Setting<Boolean> autoCast = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-cast")
            .description("Automatically casts when not fishing.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> ticksAutoCast = sgGeneral.add(new IntSetting.Builder()
            .name("ticks-auto-cast")
            .description("Ticks to wait before auto casting.")
            .defaultValue(10)
            .min(0)
            .sliderMax(60)
            .build()
    );

    private final Setting<Integer> ticksCatch = sgGeneral.add(new IntSetting.Builder()
            .name("ticks-catch")
            .description("Ticks to wait before catching the fish")
            .defaultValue(6)
            .min(0)
            .sliderMax(60)
            .build()
    );

    private final Setting<Integer> ticksThrow = sgGeneral.add(new IntSetting.Builder()
            .name("ticks-throw")
            .description("Ticks to wait before throwing the bobber.")
            .defaultValue(14)
            .min(0)
            .sliderMax(60)
            .build()
    );

    // Splash range detection
    private final Setting<Boolean> splashDetectionRangeEnabled = sgSplashRangeDetection.add(new BoolSetting.Builder()
            .name("splash-detection-range-enabled")
            .description("Allows you to use multiple accounts next to each other.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> splashDetectionRange = sgSplashRangeDetection.add(new DoubleSetting.Builder()
            .name("splash-detection-range")
            .description("Detection range of splash sound. Low values will not work when TPS is low.")
            .defaultValue(10)
            .min(0)
            .build()
    );

    private boolean ticksEnabled;
    private int ticksToRightClick;
    private int ticksData;

    private int autoCastTimer;
    private boolean autoCastEnabled;

    private int autoCastCheckTimer;

    public AutoFish() {
        super(Category.Player, "auto-fish", "Automatically fishes.");
    }

    @Override
    public void onActivate() {
        ticksEnabled = false;
        autoCastEnabled = false;
        autoCastCheckTimer = 0;
    }

    @EventHandler
    private final Listener<PlaySoundPacketEvent> onPlaySoundPacket = new Listener<>(event -> {
        PlaySoundS2CPacket p = event.packet;
        FishingBobberEntity b = mc.player.fishHook;

        if (p.getSound().getId().getPath().equals("entity.fishing_bobber.splash")) {
            if (!splashDetectionRangeEnabled.get() || Utils.distance(b.getX(), b.getY(), b.getZ(), p.getX(), p.getY(), p.getZ()) <= splashDetectionRange.get()) {
                ticksEnabled = true;
                ticksToRightClick = ticksCatch.get();
                ticksData = 0;
            }
        }
    });

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        // Auto cast
        if (autoCastCheckTimer <= 0) {
            autoCastCheckTimer = 30;

            if (autoCast.get() && !ticksEnabled && !autoCastEnabled && mc.player.fishHook == null && mc.player.getMainHandStack().getItem() instanceof FishingRodItem) {
                autoCastTimer = 0;
                autoCastEnabled = true;
            }
        } else {
            autoCastCheckTimer--;
        }

        // Check for auto cast timer
        if (autoCastEnabled) {
            autoCastTimer++;

            if (autoCastTimer > ticksAutoCast.get()) {
                autoCastEnabled = false;
                Utils.rightClick();
            }
        }

        // Handle logic
        if (ticksEnabled && ticksToRightClick <= 0) {
            if (ticksData == 0) {
                Utils.rightClick();
                ticksToRightClick = ticksThrow.get();
                ticksData = 1;
            }
            else if (ticksData == 1) {
                Utils.rightClick();
                ticksEnabled = false;
            }
        }

        ticksToRightClick--;
    });

    @EventHandler
    private final Listener<KeyEvent> onKey = new Listener<>(event -> {
        if (mc.options.keyUse.isPressed()) ticksEnabled = false;
    });
}
