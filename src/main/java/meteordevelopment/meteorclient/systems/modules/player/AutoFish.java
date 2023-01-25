/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;

public class AutoFish extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSplashRangeDetection = settings.createGroup("Splash Detection");
    private final SettingGroup sgDurability = settings.createGroup("Durability detection");

    // General

    private final Setting<Boolean> autoCast = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-cast")
            .description("Automatically casts when not fishing.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> ticksAutoCast = sgGeneral.add(new IntSetting.Builder()
            .name("ticks-auto-cast")
            .description("The amount of ticks to wait before recasting automatically.")
            .defaultValue(10)
            .min(0)
            .sliderMax(60)
            .build()
    );

    private final Setting<Integer> ticksCatch = sgGeneral.add(new IntSetting.Builder()
            .name("catch-delay")
            .description("The amount of ticks to wait before catching the fish.")
            .defaultValue(6)
            .min(0)
            .sliderMax(60)
            .build()
    );

    private final Setting<Integer> ticksThrow = sgGeneral.add(new IntSetting.Builder()
            .name("throw-delay")
            .description("The amount of ticks to wait before throwing the bobber.")
            .defaultValue(14)
            .min(0)
            .sliderMax(60)
            .build()
    );

    // Splash Detection

    private final Setting<Boolean> splashDetectionRangeEnabled = sgSplashRangeDetection.add(new BoolSetting.Builder()
            .name("splash-detection-range-enabled")
            .description("Allows you to use multiple accounts next to each other.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> splashDetectionRange = sgSplashRangeDetection.add(new DoubleSetting.Builder()
            .name("splash-detection-range")
            .description("The detection range of a splash. Lower values will not work when the TPS is low.")
            .defaultValue(10)
            .min(0)
            .build()
    );
    
    // durability detection and rod switch
    
    private final dura = sgDurability.add(new BoolSetting.Builder()
        .name("Durability")
        .description("Automatically stops and changes rods on durability low")
        .onChanged((Boolean n) -> {
            if (n) {
                    ChatUtils.sendMsg(0, "AutoFish", Formatting.DARK_RED, Formatting.GRAY, "Durability on");
            } else {
                ChatUtils.sendMsg(0, "AutoFish", Formatting.DARK_RED, Formatting.GRAY,"Durability off");
            }
        })
        .defaultValue(false)
        .build()
    );

    private final duraAmount = sgDurability.add(new IntSetting.Builder()
        .name("Durability Stop Amount")
        .description("amount used to stop and switch rods")
        .visible(dura::get)
        .defaultValue(2)
        .sliderRange(1, 64)
        .build()
    );

    private final duraSwitch = sgDurability.add(new BoolSetting.Builder()
        .name("Rod Switch")
        .description("Automatically changes rod with one that has high enough durability")
        .defaultValue(false)
        .visible(dura::get)
        .onChanged((Boolean n) -> {
            if (n) {
                ChatUtils.sendMsg(0, "AutoFish", Formatting.DARK_RED, Formatting.GRAY, "Durability switch on");
            } else {
                ChatUtils.sendMsg(0, "AutoFish", Formatting.DARK_RED, Formatting.GRAY, "Durability switch off");
            }
        })
        .build()
    );

    private final duraSwitchSlot = sgDurability.add(new IntSetting.Builder()
        .name("Rod Switch Slot")
        .description("the slot the rod switches")
        .visible(() -> dura.get() && duraSwitch.get())
        .defaultValue(0)
        .sliderRange(0, 9)
        .build()
    );

    private boolean ticksEnabled;
    private int ticksToRightClick;
    private int ticksData;

    private int autoCastTimer;
    private boolean autoCastEnabled;

    private int autoCastCheckTimer;

    public AutoFish() {
        super(Categories.Player, "auto-fish", "Automatically fishes for you.");
    }

    @Override
    public void onActivate() {
        ticksEnabled = false;
        autoCastEnabled = false;
        autoCastCheckTimer = 0;
    }

    @EventHandler
    private void onPlaySound(PlaySoundEvent event) {
        SoundInstance p = event.sound;
        FishingBobberEntity b = mc.player.fishHook;

        if (p.getId().getPath().equals("entity.fishing_bobber.splash")) {
            if (!splashDetectionRangeEnabled.get() || Utils.distance(b.getX(), b.getY(), b.getZ(), p.getX(), p.getY(), p.getZ()) <= splashDetectionRange.get()) {
                ticksEnabled = true;
                ticksToRightClick = ticksCatch.get();
                ticksData = 0;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // rod switch and durability stop < unoptimized code
        PlayerInventory inventory = mc.player.getInventory();
        if (dura.get()) {
            if (duraSwitch.get()) {
                ItemStack currentRod = inventory.getStack(duraSwitchSlot.get());
                if (!currentRod.isOf(Items.FISHING_ROD) || currentRod.getMaxDamage() - currentRod.getDamage() <= duraAmount.get()) {
                    boolean foundHigherDurabilityRod = false;
                    for (int count = 0; count < inventory.size(); count++) {
                        ItemStack stack = inventory.getStack(count);
                        if (stack.isOf(Items.FISHING_ROD) && stack.getMaxDamage() - stack.getDamage() > currentRod.getMaxDamage() - currentRod.getDamage()) {
                            foundHigherDurabilityRod = true;
                            ChatUtils.sendMsg(0, "AutoFish", Formatting.DARK_RED, Formatting.GRAY, "Durability switch item found: switching slots");
                            InvUtils.move().from(count).to(duraSwitchSlot.get());
                            InvUtils.swap(duraSwitchSlot.get(), false);
                            break;
                        }
                    }
                    if (!foundHigherDurabilityRod) {
                        ChatUtils.sendMsg(0, "AutoFish", Formatting.DARK_RED, Formatting.GRAY, "Cannot find another rod with higher durability: stopping");
                        toggle();
                    }
                }
            } else {
                ItemStack currentRod = mc.player.getMainHandStack();
                if (!currentRod.isOf(Items.FISHING_ROD)) {
                    ChatUtils.sendMsg(0, "AutoFish", Formatting.DARK_RED, Formatting.GRAY, "Not holding a fishing rod: stopping");
                    toggle();
                }
                else if (currentRod.getMaxDamage() - currentRod.getDamage() <= duraAmount.get()) {
                    ChatUtils.sendMsg(0, "AutoFish", Formatting.DARK_RED, Formatting.GRAY, "Durability matches setting: stopping");
                    toggle();
                }
            }
        }
        
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
                if (dura.get() && duraSwitch.get()) {
                    if (mc.player.getInventory().selectedSlot != duraSwitchSlot.get()) {
                        ChatUtils.sendMsg(0, "AutoFish", Formatting.DARK_RED, Formatting.GRAY, "Switching selected slot to slot in settings");
                        mc.player.getInventory().selectedSlot = duraSwitchSlot.get();
                    }
                }
                Utils.rightClick();
                ticksToRightClick = ticksThrow.get();
                ticksData = 1;
            }
            else if (ticksData == 1) {
                if (dura.get() && duraSwitch.get()) {
                    if (mc.player.getInventory().selectedSlot != duraSwitchSlot.get()) {
                        ChatUtils.sendMsg(0, "AutoFish", Formatting.DARK_RED, Formatting.GRAY, "Switching selected slot to slot in settings");
                        mc.player.getInventory().selectedSlot = duraSwitchSlot.get();
                    }
                }
                Utils.rightClick();
                ticksEnabled = false;
            }
        }

        ticksToRightClick--;
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (mc.options.useKey.isPressed()) ticksEnabled = false;
    }
}
