/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractEntityEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class NoInteract extends Module {
    private final SettingGroup sgBlocks = settings.createGroup("Blocks");
    private final SettingGroup sgEntities = settings.createGroup("Entities");

    // Blocks

    private final Setting<List<Block>> blockMine = sgBlocks.add(new BlockListSetting.Builder()
        .name("block-mine")
        .description("Cancels block mining.")
        .build()
    );

    private final Setting<ListMode> blockMineMode = sgBlocks.add(new EnumSetting.Builder<ListMode>()
        .name("block-mine-mode")
        .description("List mode to use for block mine.")
        .defaultValue(ListMode.BlackList)
        .build()
    );

    private final Setting<List<Block>> blockInteract = sgBlocks.add(new BlockListSetting.Builder()
        .name("block-interact")
        .description("Cancels block interaction.")
        .build()
    );

    private final Setting<ListMode> blockInteractMode = sgBlocks.add(new EnumSetting.Builder<ListMode>()
        .name("block-interact-mode")
        .description("List mode to use for block interact.")
        .defaultValue(ListMode.BlackList)
        .build()
    );

    private final Setting<HandMode> blockInteractHand = sgBlocks.add(new EnumSetting.Builder<HandMode>()
        .name("block-interact-hand")
        .description("Cancels block interaction if performed by this hand.")
        .defaultValue(HandMode.None)
        .build()
    );

    // Entities

    private final Setting<Object2BooleanMap<EntityType<?>>> entityHit = sgEntities.add(new EntityTypeListSetting.Builder()
        .name("entity-hit")
        .description("Cancel entity hitting.")
        .onlyAttackable()
        .build()
    );

    private final Setting<ListMode> entityHitMode = sgEntities.add(new EnumSetting.Builder<ListMode>()
        .name("entity-hit-mode")
        .description("List mode to use for entity hit.")
        .defaultValue(ListMode.BlackList)
        .build()
    );

    private final Setting<Object2BooleanMap<EntityType<?>>> entityInteract = sgEntities.add(new EntityTypeListSetting.Builder()
        .name("entity-interact")
        .description("Cancel entity interaction.")
        .onlyAttackable()
        .build()
    );

    private final Setting<ListMode> entityInteractMode = sgEntities.add(new EnumSetting.Builder<ListMode>()
        .name("entity-interact-mode")
        .description("List mode to use for entity interact.")
        .defaultValue(ListMode.BlackList)
        .build()
    );

    private final Setting<HandMode> entityInteractHand = sgEntities.add(new EnumSetting.Builder<HandMode>()
        .name("entity-interact-hand")
        .description("Cancels entity interaction if performed by this hand.")
        .defaultValue(HandMode.None)
        .build()
    );

    private final Setting<InteractMode> friends = sgEntities.add(new EnumSetting.Builder<InteractMode>()
        .name("friends")
        .description("Friends cancel mode.")
        .defaultValue(InteractMode.None)
        .build()
    );

    private final Setting<InteractMode> babies = sgEntities.add(new EnumSetting.Builder<InteractMode>()
        .name("babies")
        .description("Baby entity cancel mode.")
        .defaultValue(InteractMode.None)
        .build()
    );

    private final Setting<InteractMode> nametagged = sgEntities.add(new EnumSetting.Builder<InteractMode>()
        .name("nametagged")
        .description("Nametagged entity cancel mode.")
        .defaultValue(InteractMode.None)
        .build()
    );

    public NoInteract() {
        super(Categories.Player, "no-interact", "Blocks interactions with certain types of inputs.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlockEvent(StartBreakingBlockEvent event) {
        if (!shouldAttackBlock(event.blockPos)) event.cancel();
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        if (!shouldInteractBlock(event.result, event.hand)) event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onAttackEntity(AttackEntityEvent event) {
        if (!shouldAttackEntity(event.entity)) event.cancel();
    }

    @EventHandler
    private void onInteractEntity(InteractEntityEvent event) {
       if (!shouldInteractEntity(event.entity, event.hand)) event.cancel();
    }

    private boolean shouldAttackBlock(BlockPos blockPos) {
        if (blockMineMode.get() == ListMode.WhiteList &&
            blockMine.get().contains(mc.world.getBlockState(blockPos).getBlock())) {
            return false;
        }

        return blockMineMode.get() != ListMode.BlackList ||
            !blockMine.get().contains(mc.world.getBlockState(blockPos).getBlock());
    }

    private boolean shouldInteractBlock(BlockHitResult hitResult, Hand hand) {
        // Hand Interactions
        if (blockInteractHand.get() == HandMode.Both ||
            (blockInteractHand.get() == HandMode.Mainhand && hand == Hand.MAIN_HAND) ||
            (blockInteractHand.get() == HandMode.Offhand && hand == Hand.OFF_HAND)) {
            return false;
        }

        // Blocks
        if (blockInteractMode.get() == ListMode.BlackList &&
            blockInteract.get().contains(mc.world.getBlockState(hitResult.getBlockPos()).getBlock())) {
            return false;
        }

        return blockInteractMode.get() != ListMode.WhiteList ||
            blockInteract.get().contains(mc.world.getBlockState(hitResult.getBlockPos()).getBlock());
    }

    private boolean shouldAttackEntity(Entity entity) {
        // Friends
        if ((friends.get() == InteractMode.Both || friends.get() == InteractMode.Hit) &&
            entity instanceof PlayerEntity && !Friends.get().shouldAttack((PlayerEntity) entity)) {
            return false;
        }

        // Babies
        if ((babies.get() == InteractMode.Both || babies.get() == InteractMode.Hit) &&
            entity instanceof AnimalEntity && ((AnimalEntity) entity).isBaby()) {
            return false;
        }

        // NameTagged
        if ((nametagged.get() == InteractMode.Both || nametagged.get() == InteractMode.Hit) && entity.hasCustomName()) return false;

        // Entities
        if (entityHitMode.get() == ListMode.BlackList &&
            entityHit.get().getBoolean(entity.getType())) {
            return false;
        }

        else return entityHitMode.get() != ListMode.WhiteList ||
            entityHit.get().getBoolean(entity.getType());
    }

    private boolean shouldInteractEntity(Entity entity, Hand hand) {
        // Hand Interactions
        if (entityInteractHand.get() == HandMode.Both ||
            (entityInteractHand.get() == HandMode.Mainhand && hand == Hand.MAIN_HAND) ||
            (entityInteractHand.get() == HandMode.Offhand && hand == Hand.OFF_HAND)) {
            return false;
        }

        // Friends
        if ((friends.get() == InteractMode.Both || friends.get() == InteractMode.Interact) &&
            entity instanceof PlayerEntity && !Friends.get().shouldAttack((PlayerEntity) entity)) {
            return false;
        }

        // Babies
        if ((babies.get() == InteractMode.Both || babies.get() == InteractMode.Interact) &&
            entity instanceof AnimalEntity && ((AnimalEntity) entity).isBaby()) {
            return false;
        }

        // NameTagged
        if ((nametagged.get() == InteractMode.Both || nametagged.get() == InteractMode.Interact) && entity.hasCustomName()) return false;

        // Entities
        if (entityInteractMode.get() == ListMode.BlackList &&
            entityInteract.get().getBoolean(entity.getType())) {
            return false;
        }
        else return entityInteractMode.get() != ListMode.WhiteList ||
            entityInteract.get().getBoolean(entity.getType());
    }

    public enum HandMode {
        Mainhand,
        Offhand,
        Both,
        None
    }

    public enum ListMode {
        WhiteList,
        BlackList
    }

    public enum InteractMode {
        Hit,
        Interact,
        Both,
        None
    }
}
