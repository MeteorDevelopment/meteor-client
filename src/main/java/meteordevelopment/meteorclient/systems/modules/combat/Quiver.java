/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.BlockBehaviourAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Quiver extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSafety = settings.createGroup("Safety");


    private final Setting<List<MobEffect>> effects = sgGeneral.add(new StatusEffectListSetting.Builder()
        .name("effects")
        .description("Which effects to shoot you with.")
        .defaultValue(MobEffects.STRENGTH.value())
        .build()
    );

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("How many ticks between shooting effects (19 minimum for NCP).")
        .defaultValue(10)
        .range(0, 40)
        .sliderRange(0, 40)
        .build()
    );

    private final Setting<Boolean> checkEffects = sgGeneral.add(new BoolSetting.Builder()
        .name("check-effects")
        .description("Won't shoot you with effects you already have.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> silentBow = sgGeneral.add(new BoolSetting.Builder()
        .name("silent-bow")
        .description("Takes a bow from your inventory to quiver.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-info")
        .description("Sends info about quiver checks in chat.")
        .defaultValue(false)
        .build()
    );

    // Safety

    private final Setting<Boolean> onlyInHoles = sgSafety.add(new BoolSetting.Builder()
        .name("only-in-holes")
        .description("Only quiver when you're in a hole.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgSafety.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Only quiver when you're on the ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> minHealth = sgSafety.add(new DoubleSetting.Builder()
        .name("min-health")
        .description("How much health you must have to quiver.")
        .defaultValue(10)
        .range(0, 36)
        .sliderRange(0, 36)
        .build()
    );

    private final List<Integer> arrowSlots = new ArrayList<>();
    private FindItemResult bow;
    private boolean wasMainhand, wasHotbar;
    private int timer, prevSlot;
    private final BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos();

    public Quiver() {
        super(Categories.Combat, "quiver", "Shoots arrows at yourself.");
    }

    @Override
    public void onActivate() {
        bow = InvUtils.find(Items.BOW);
        if (!shouldQuiver()) return;

        mc.options.keyUse.setDown(false);
        mc.gameMode.releaseUsingItem(mc.player);

        prevSlot = bow.slot();
        wasHotbar = bow.isHotbar();
        timer = 0;

        if (!bow.isMainHand()) {
            if (wasHotbar) InvUtils.swap(bow.slot(), true);
            else InvUtils.move().from(mc.player.getInventory().getSelectedSlot()).to(prevSlot);
        } else wasMainhand = true;

        arrowSlots.clear();
        List<MobEffect> usedEffects = new ArrayList<>();

        for (int i = mc.player.getInventory().getContainerSize(); i > 0; i--) {
            if (i == mc.player.getInventory().getSelectedSlot()) continue;

            ItemStack item = mc.player.getInventory().getItem(i);

            if (item.getItem() != Items.TIPPED_ARROW) continue;

            Iterator<MobEffectInstance> effects = item.getItem().components().get(DataComponents.POTION_CONTENTS).getAllEffects().iterator();

            if (!effects.hasNext()) continue;

            MobEffect effect = effects.next().getEffect().value();

            if (this.effects.get().contains(effect)
                && !usedEffects.contains(effect)
                && (!hasEffect(effect) || !checkEffects.get())) {
                usedEffects.add(effect);
                arrowSlots.add(i);
            }
        }
    }

    @Override
    public void onDeactivate() {
        if (!wasMainhand) {
            if (wasHotbar) InvUtils.swapBack();
            else InvUtils.move().from(mc.player.getInventory().getSelectedSlot()).to(prevSlot);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        bow = InvUtils.find(Items.BOW);
        if (!shouldQuiver()) return;
        if (arrowSlots.isEmpty()) {
            toggle();
            return;
        }

        if (timer > 0) {
            timer--;
            return;
        }

        boolean charging = mc.options.keyUse.isDown();

        if (!charging) {
            InvUtils.move().from(arrowSlots.getFirst()).to(9);
            mc.options.keyUse.setDown(true);
        } else {
            if (BowItem.getPowerForTime(mc.player.getTicksUsingItem()) >= 0.12) {
                int targetSlot = arrowSlots.getFirst();
                arrowSlots.removeFirst();

                mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(mc.player.getYRot(), -90, mc.player.onGround(), mc.player.horizontalCollision));
                mc.options.keyUse.setDown(false);
                mc.gameMode.releaseUsingItem(mc.player);
                if (targetSlot != 9) InvUtils.move().from(9).to(targetSlot);

                timer = cooldown.get();
            }
        }
    }

    private boolean shouldQuiver() {
        if (!bow.found() || !bow.isHotbar() && !silentBow.get()) {
            if (chatInfo.get()) error("Couldn't find a usable bow, disabling.");
            toggle();
            return false;
        }

        if (!headIsOpen()) {
            if (chatInfo.get()) error("Not enough space to quiver, disabling.");
            toggle();
            return false;
        }

        if (EntityUtils.getTotalHealth(mc.player) < minHealth.get()) {
            if (chatInfo.get()) error("Not enough health to quiver, disabling.");
            toggle();
            return false;
        }

        if (onlyOnGround.get() && !mc.player.onGround()) {
            if (chatInfo.get()) error("You are not on the ground, disabling.");
            toggle();
            return false;
        }

        if (onlyInHoles.get() && !isSurrounded(mc.player)) {
            if (chatInfo.get()) error("You are not in a hole, disabling.");
            toggle();
            return false;
        }

        return true;
    }

    private boolean headIsOpen() {
        testPos.set(mc.player.blockPosition().offset(0, 1, 0));
        BlockState pos1 = mc.level.getBlockState(testPos);
        if (((BlockBehaviourAccessor) pos1.getBlock()).meteor$isHasCollision()) return false;

        testPos.offset(0, 1, 0);
        BlockState pos2 = mc.level.getBlockState(testPos);
        return !((BlockBehaviourAccessor) pos2.getBlock()).meteor$isHasCollision();
    }

    private boolean hasEffect(MobEffect effect) {
        for (MobEffectInstance statusEffect : mc.player.getActiveEffects()) {
            if (statusEffect.getEffect().value().equals(effect)) return true;
        }

        return false;
    }

    private boolean isSurrounded(Player target) {
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;

            testPos.set(target.blockPosition()).relative(dir);
            Block block = mc.level.getBlockState(testPos).getBlock();

            if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK && block != Blocks.RESPAWN_ANCHOR
                && block != Blocks.CRYING_OBSIDIAN && block != Blocks.NETHERITE_BLOCK) {
                return false;
            }
        }

        return true;
    }
}
