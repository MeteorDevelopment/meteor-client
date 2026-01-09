/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.HorizontalDirection;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

public class Flamethrower extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
        .name("distance")
        .min(0.0)
        .defaultValue(5.0)
        .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-break")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> putOutFire = sgGeneral.add(new BoolSetting.Builder()
        .name("put-out-fire")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> targetBabies = sgGeneral.add(new BoolSetting.Builder()
        .name("target-babies")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> tickInterval = sgGeneral.add(new IntSetting.Builder()
        .name("tick-interval")
        .defaultValue(5)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .defaultValue(true)
        .build()
    );

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .defaultValue(
            EntityType.PIG,
            EntityType.COW,
            EntityType.SHEEP,
            EntityType.CHICKEN,
            EntityType.RABBIT
        )
        .build()
    );

    private Entity entity;
    private int ticks = 0;
    private Hand hand;

    public Flamethrower() {
        super(Categories.World, "flamethrower");
    }

    @Override
    public void onDeactivate() {
        entity = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        entity = null;
        ticks++;
        for (Entity entity : mc.world.getEntities()) {
            if (!entities.get().contains(entity.getType()) || !PlayerUtils.isWithin(entity, distance.get())) continue;
            if (entity == mc.player) continue;
            if (!entity.isAlive() || entity.inPowderSnow || entity.isTouchingWaterOrRain() || entity.isFireImmune()) continue;

            if (!targetBabies.get() && entity instanceof LivingEntity livingEntity && livingEntity.isBaby()) continue;

            FindItemResult item = InvUtils.findInHotbar(itemStack -> (itemStack.isOf(Items.FLINT_AND_STEEL) || itemStack.isOf(Items.FIRE_CHARGE)) &&
                (!itemStack.isDamageable() || !antiBreak.get() || itemStack.getDamage() < itemStack.getMaxDamage() - 1));
            if (!InvUtils.swap(item.slot(), true)) return;

            this.hand = item.getHand();
            this.entity = entity;

            if (rotate.get()) Rotations.rotate(Rotations.getYaw(entity.getBlockPos()), Rotations.getPitch(entity.getBlockPos()), -100, this::interact);
            else interact();

            return;
        }
    }

    private void interact() {
        Block block = mc.world.getBlockState(entity.getBlockPos()).getBlock();
        Block bottom = mc.world.getBlockState(entity.getBlockPos().down()).getBlock();
        if (block == Blocks.WATER || bottom == Blocks.WATER || bottom == Blocks.DIRT_PATH) return;
        if (block == Blocks.GRASS_BLOCK)  mc.interactionManager.attackBlock(entity.getBlockPos(), Direction.DOWN);

        if (putOutFire.get() && entity instanceof LivingEntity animal && animal.getHealth() < 2) {
            mc.interactionManager.attackBlock(entity.getBlockPos(), Direction.DOWN);
            for (HorizontalDirection direction : HorizontalDirection.values()) {
                mc.interactionManager.attackBlock(entity.getBlockPos().add(direction.offsetX, 0, direction.offsetZ), Direction.DOWN);
            }
        } else {
            if (ticks >= tickInterval.get() && !entity.isOnFire()) {
                mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(
                    entity.getEntityPos().subtract(new Vec3d(0, 1, 0)), Direction.UP, entity.getBlockPos().down(), false));
                ticks = 0;
            }
        }

        InvUtils.swapBack();
    }
}
