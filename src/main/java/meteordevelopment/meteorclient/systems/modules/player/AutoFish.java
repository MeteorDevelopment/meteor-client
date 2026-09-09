/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.FishingHookAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

public class AutoFish extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Automatically switch to a fishing rod.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-break")
        .description("Avoid using rods that would break if they were cast.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoCast = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-cast")
        .description("Automatically cast the fishing rod.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> castDelay = sgGeneral.add(new IntSetting.Builder()
        .name("cast-delay")
        .description("How long to wait between recasts if the bobber fails to land in water.")
        .defaultValue(14)
        .min(1)
        .sliderMax(60)
        .build()
    );

    private final Setting<Integer> castDelayVariance = sgGeneral.add(new IntSetting.Builder()
        .name("cast-delay-variance")
        .description("Maximum amount of randomness added to cast delay.")
        .defaultValue(0)
        .min(0)
        .sliderMax(30)
        .build()
    );

    private final Setting<Integer> catchDelay = sgGeneral.add(new IntSetting.Builder()
        .name("catch-delay")
        .description("How long to wait after hooking a fish to reel it in.")
        .defaultValue(6)
        .min(1)
        .sliderMax(20)
        .build()
    );

    private final Setting<Integer> catchDelayVariance = sgGeneral.add(new IntSetting.Builder()
        .name("catch-delay-variance")
        .description("Maximum amount of randomness added to catch delay.")
        .defaultValue(0)
        .min(0)
        .sliderMax(10) // Since the shortest Java edition catch window is 20 ticks, this is the highest possible variance that won't miss fish.
        .build()
    );

    private enum State {
        IDLE,
        WAITING_FOR_BITE,
        WAITING_TO_REEL
    }

    private State state = State.IDLE;
    private double castDelayLeft = 0.0;
    private double catchDelayLeft = 0.0;

    public AutoFish() {
        super(Categories.Player, "auto-fish", "Automatically fishes for you.");
    }

    @Override
    public void onActivate() {
        state = State.IDLE;
        castDelayLeft = 0.0;
        catchDelayLeft = 0.0;
    }

    @Override
    public void onDeactivate() {
        state = State.IDLE;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player.fishing != null) handleFishing();
        else handleIdle();
    }

    private void handleIdle() {
        if (state != State.IDLE) state = State.IDLE;

        InteractionHand fishingHand = getRodHand();

        if (fishingHand == null) {
            int bestRodSlot = findBestRod();

            if (autoSwitch.get() && bestRodSlot != -1 && mc.player.getInventory().getSelectedSlot() != bestRodSlot) {
                InvUtils.swap(bestRodSlot, false);
            }

            fishingHand = getRodHand();
        }

        if (fishingHand == null) return;
        if (!autoCast.get()) return;
        if (castDelayLeft > 0) {
            castDelayLeft -= TickRate.INSTANCE.getTickRate() / 20.0;
            return;
        }

        cast();
    }

    private void handleFishing() {
        switch (state) {
            case IDLE -> state = State.WAITING_FOR_BITE;
            case WAITING_FOR_BITE -> {
                if (mc.player.fishing.getHookedIn() != null) {
                    reel();
                    return;
                }

                if (mc.player.fishing.currentState != FishingHook.FishHookState.BOBBING) return;

                if (((FishingHookAccessor) mc.player.fishing).meteor$hasCaughtFish()) {
                    catchDelayLeft = randomizeDelay(catchDelay.get(), catchDelayVariance.get());
                    state = State.WAITING_TO_REEL;
                }
            }
            case WAITING_TO_REEL -> {
                if (mc.player.fishing.getHookedIn() != null) {
                    reel();
                    return;
                }

                if (catchDelayLeft > 0) {
                    catchDelayLeft -= TickRate.INSTANCE.getTickRate() / 20.0;
                    return;
                }

                reel();
            }
        }
    }

    private void cast() {
        if (useRod()) state = State.WAITING_FOR_BITE;
    }

    private void reel() {
        if (useRod()) state = State.IDLE;
    }

    private boolean useRod() {
        InteractionHand fishingHand = getRodHand();
        if (fishingHand == null) return false;

        mc.gameMode.useItem(mc.player, fishingHand);
        castDelayLeft = randomizeDelay(castDelay.get(), castDelayVariance.get());
        return true;
    }

    private InteractionHand getRodHand() {
        if (isUsableRod(mc.player.getOffhandItem())) return InteractionHand.OFF_HAND;
        if (isUsableRod(mc.player.getMainHandItem())) return InteractionHand.MAIN_HAND;

        return null;
    }

    private boolean isUsableRod(ItemStack stack) {
        if (!(stack.getItem() instanceof FishingRodItem)) return false;
        return !antiBreak.get() || stack.getDamageValue() != stack.getMaxDamage() - 1;
    }

    private int findBestRod() {
        int bestSlot = -1;
        int bestScore = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!isUsableRod(stack)) continue;

            int score = 0;

            score += Utils.getEnchantmentLevel(stack, Enchantments.LUCK_OF_THE_SEA);
            score += Utils.getEnchantmentLevel(stack, Enchantments.LURE);
            score += Utils.getEnchantmentLevel(stack, Enchantments.MENDING);
            score += Utils.getEnchantmentLevel(stack, Enchantments.UNBREAKING);

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }

            // Found a maxed out rod
            if (score == 10) break;
        }

        return bestSlot;
    }

    private double randomizeDelay(int delay, int variance) {
        if (variance == 0) return delay;

        // Sample the standard normal distribution via Box-Muller transform
        double scale = Math.sqrt(-2 * Math.log(Utils.random(0.0001, 1.0)));
        double angle = Math.TAU * Utils.random(0.0, 1.0);
        double norm = scale * Math.cos(angle);

        // Clamp to 3 standard deviations and re-scale to [-3.0, +3.0]
        final double MAX_SD = 3.0;
        norm = Math.clamp(norm, -MAX_SD, MAX_SD) / MAX_SD;

        delay += Math.round((float) (norm * variance));
        return Math.max(1, delay);
    }
}
