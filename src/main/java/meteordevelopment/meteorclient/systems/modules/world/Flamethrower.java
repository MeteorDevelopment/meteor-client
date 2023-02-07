/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
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
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Flamethrower extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<RoastItem> roastItem = sgGeneral.add(new EnumSetting.Builder<RoastItem>()
        .name("roast-item")
        .description("The item to be used for roasting. Jesus recommended with Lava bucket.")
        .defaultValue(RoastItem.FlintSteel)
        .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-break")
        .description("Prevents Flint and Steel from being broken.")
        .defaultValue(false)
        .visible(() -> roastItem.get() == RoastItem.FlintSteel)
        .build()
    );

    private final Setting<Boolean> recollectLava = sgGeneral.add(new BoolSetting.Builder()
        .name("recollect-lava")
        .description("Automatically recollects the Lava after placing.")
        .defaultValue(true)
        .visible(() -> roastItem.get() == RoastItem.LavaBucket)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically faces towards the animal while roasting.")
        .defaultValue(true)
        .visible(() -> roastItem.get() != RoastItem.LavaBucket)
        .build()
    );

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
        .name("distance")
        .description("The maximum distance the animal has to be to be roasted.")
        .min(0.0)
        .defaultValue(5.0)
        .build()
    );

    private final Setting<Boolean> putOutFire = sgGeneral.add(new BoolSetting.Builder()
        .name("put-out-fire")
        .description("Tries to put out the fire when animal is at low health, so the items don't burn.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> targetBabies = sgGeneral.add(new BoolSetting.Builder()
        .name("target-babies")
        .description("If checked, babies will also be killed.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> tickInterval = sgGeneral.add(new IntSetting.Builder()
        .name("tick-interval")
        .description("Minimum delay in ticks for igniting the entity.")
        .defaultValue(10)
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

    private BlockPos lavaPos;
    private int ticks = 0;

    public Flamethrower() {
        super(Categories.World, "flamethrower", "Ignites every alive piece of food.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        ticks++;

        // Recollect lava
        if (recollectLava.get() && roastItem.get() == RoastItem.LavaBucket &&
            (lavaPos != null && mc.world.getBlockState(lavaPos).getBlock() == Blocks.LAVA)) {
            interact(InvUtils.find(Items.BUCKET), lavaPos, true, true);
            lavaPos = null;
        }

        Entity entity = findEntity();
        // Continue only if we found a valid entity
        if (entity == null) return;

        // Put out fire if entity is at low health
        if (putOutFire.get() && entity instanceof LivingEntity animal && animal.getHealth() < 1) {
            mc.interactionManager.attackBlock(entity.getBlockPos(), Direction.DOWN);
            for (HorizontalDirection direction : HorizontalDirection.values()) {
                mc.interactionManager.attackBlock(entity.getBlockPos().add(direction.offsetX, 0, direction.offsetZ), Direction.DOWN);
            }
        } else if (ticks >= tickInterval.get() && !entity.isOnFire()) {
            // Finally, ignite the entity with fire
            FindItemResult itemResult = InvUtils.findInHotbar(itemStack -> itemStack.isOf(roastItem.get().item) &&
                (!itemStack.isDamageable() || (!antiBreak.get() || itemStack.getDamage() < itemStack.getMaxDamage() - 1)));

            if (roastItem.get() == RoastItem.LavaBucket) {
                // Place lava if we can see the feet of entity
                if (!PlayerUtils.canSeeFeet(entity)) return;

                lavaPos = entity.getBlockPos();
                interact(itemResult, lavaPos, true, true);
            } else {
                // Else interact with the block using flammable item
                interact(itemResult, entity.getBlockPos(), false, rotate.get());
            }

            ticks = 0;
        }
    }

    private Entity findEntity() {
        for (Entity entity : mc.world.getEntities()) {
            if (!entities.get().getBoolean(entity.getType()) || !PlayerUtils.isWithin(entity, distance.get())) continue;
            if (entity.isFireImmune()) continue;
            if (entity == mc.player) continue;
            if (!targetBabies.get() && entity instanceof LivingEntity && ((LivingEntity)entity).isBaby()) continue;

            return entity;
        }

        return null;
    }

    private void interact(FindItemResult itemResult, BlockPos targetPos, boolean interactItem) {
        if (!InvUtils.swap(itemResult.slot(), true)) return;

        Block block = mc.world.getBlockState(targetPos).getBlock();
        Block bottom = mc.world.getBlockState(targetPos.down()).getBlock();
        if (block == Blocks.WATER || bottom == Blocks.WATER || bottom == Blocks.DIRT_PATH) return;
        if (block == Blocks.GRASS) mc.interactionManager.attackBlock(targetPos, Direction.DOWN);

        if (interactItem) {
            mc.interactionManager.interactItem(mc.player, itemResult.getHand());
        } else {
            mc.interactionManager.interactBlock(mc.player, itemResult.getHand(), new BlockHitResult(
                targetPos.down().toCenterPos(), Direction.UP, targetPos.down(), false));
        }

        InvUtils.swapBack();
    }

    private void interact(FindItemResult itemResult, BlockPos targetPos, boolean interactItem, boolean rotate) {
        if (rotate) {
            Rotations.rotate(Rotations.getYaw(Vec3d.ofBottomCenter(targetPos)), Rotations.getPitch(Vec3d.ofBottomCenter(targetPos)), -100,
                () -> interact(itemResult, targetPos, interactItem));
        } else interact(itemResult, targetPos, interactItem);
    }

    public enum RoastItem {
        FireCharge(Items.FIRE_CHARGE),
        FlintSteel(Items.FLINT_AND_STEEL),
        LavaBucket(Items.LAVA_BUCKET);

        Item item;

        RoastItem(Item item) {
            this.item = item;
        }
    }
}
