/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
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

public class Flamethrower extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
        .name("distance")
        .description("The maximum distance the animal has to be to be roasted.")
        .min(0.0)
        .defaultValue(5.0)
        .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-break")
        .description("Prevents flint and steel from being broken.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> putOutFire = sgGeneral.add(new BoolSetting.Builder()
        .name("put-out-fire")
        .description("Tries to put out the fire when animal is low health, so the items don't burn.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> targetBabies = sgGeneral.add(new BoolSetting.Builder()
        .name("target-babies")
        .description("If checked babies will also be killed.")
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
            .description("Automatically faces towards the animal roasted.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to cook.")
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

    public Flamethrower() {
        super(Categories.World, "flamethrower", "Ignites every alive piece of food.");
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
            if (!entities.get().getBoolean(entity.getType()) || mc.player.distanceTo(entity) > distance.get()) continue;
            if (entity.isFireImmune()) continue;
            if (entity == mc.player) continue;
            if (!targetBabies.get() && entity instanceof LivingEntity && ((LivingEntity)entity).isBaby()) continue;

            boolean success = selectSlot();

            if (success) {
                this.entity = entity;

                if (rotate.get()) Rotations.rotate(Rotations.getYaw(entity.getBlockPos()), Rotations.getPitch(entity.getBlockPos()), -100, this::interact);
                else interact();

                return;
            }
        }
    }

    private void interact() {
        Block block = mc.world.getBlockState(entity.getBlockPos()).getBlock();
        Block bottom = mc.world.getBlockState(entity.getBlockPos().down()).getBlock();
        if (block == Blocks.WATER || bottom == Blocks.WATER || bottom == Blocks.DIRT_PATH) return;
        if (block == Blocks.GRASS)  mc.interactionManager.attackBlock(entity.getBlockPos(), Direction.DOWN);

        if (putOutFire.get() && entity instanceof LivingEntity animal && animal.getHealth() < 1) {
            mc.interactionManager.attackBlock(entity.getBlockPos(), Direction.DOWN);
            mc.interactionManager.attackBlock(entity.getBlockPos().west(), Direction.DOWN);
            mc.interactionManager.attackBlock(entity.getBlockPos().east(), Direction.DOWN);
            mc.interactionManager.attackBlock(entity.getBlockPos().north(), Direction.DOWN);
            mc.interactionManager.attackBlock(entity.getBlockPos().south(), Direction.DOWN);
        } else {
            if (ticks >= tickInterval.get() && !entity.isOnFire()) {
                mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(
                    entity.getPos().subtract(new Vec3d(0, 1, 0)), Direction.UP, entity.getBlockPos().down(), false));
                ticks = 0;
            }
        }

        InvUtils.swapBack();
    }

    private boolean selectSlot() {
        boolean findNewFlintAndSteel = false;
        if (mc.player.getInventory().getMainHandStack().getItem() == Items.FLINT_AND_STEEL) {
            if (antiBreak.get() && mc.player.getInventory().getMainHandStack().getDamage() >= mc.player.getInventory().getMainHandStack().getMaxDamage() - 1)
                findNewFlintAndSteel = true;
        } else if (mc.player.getInventory().offHand.get(0).getItem() == Items.FLINT_AND_STEEL) {
            if (antiBreak.get() && mc.player.getInventory().offHand.get(0).getDamage() >= mc.player.getInventory().offHand.get(0).getMaxDamage() - 1)
                findNewFlintAndSteel = true;
        } else {
            findNewFlintAndSteel = true;
        }

        boolean foundFlintAndSteel = !findNewFlintAndSteel;
        if (findNewFlintAndSteel) {
            foundFlintAndSteel = InvUtils.swap(InvUtils.findInHotbar(itemStack -> (!antiBreak.get() || (antiBreak.get() && itemStack.getDamage() < itemStack.getMaxDamage() - 1)) && itemStack.getItem() == Items.FLINT_AND_STEEL).slot(), true);
        }
        return foundFlintAndSteel;
    }
}
