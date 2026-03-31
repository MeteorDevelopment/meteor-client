/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Set;

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

    private final Setting<Set<EntityType<?>>> entityHit = sgEntities.add(new EntityTypeListSetting.Builder()
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

    private final Setting<Set<EntityType<?>>> entityInteract = sgEntities.add(new EntityTypeListSetting.Builder()
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

    private boolean shouldInteractBlock(BlockHitResult hitResult, InteractionHand hand) {
        // Hand Interactions
        if (blockInteractHand.get() == HandMode.Both ||
            (blockInteractHand.get() == HandMode.Mainhand && hand == InteractionHand.MAIN_HAND) ||
            (blockInteractHand.get() == HandMode.Offhand && hand == InteractionHand.OFF_HAND)) {
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
            entity instanceof Player && !Friends.get().shouldAttack((Player) entity)) {
            return false;
        }

        // Babies
        if ((babies.get() == InteractMode.Both || babies.get() == InteractMode.Hit) &&
            entity instanceof Animal && ((Animal) entity).isBaby()) {
            return false;
        }

        // NameTagged
        if ((nametagged.get() == InteractMode.Both || nametagged.get() == InteractMode.Hit) && entity.hasCustomName())
            return false;

        // Entities
        if (entityHitMode.get() == ListMode.BlackList &&
            entityHit.get().contains(entity.getType())) {
            return false;
        } else return entityHitMode.get() != ListMode.WhiteList ||
            entityHit.get().contains(entity.getType());
    }

    private boolean shouldInteractEntity(Entity entity, InteractionHand hand) {
        // Hand Interactions
        if (entityInteractHand.get() == HandMode.Both ||
            (entityInteractHand.get() == HandMode.Mainhand && hand == InteractionHand.MAIN_HAND) ||
            (entityInteractHand.get() == HandMode.Offhand && hand == InteractionHand.OFF_HAND)) {
            return false;
        }

        // Friends
        if ((friends.get() == InteractMode.Both || friends.get() == InteractMode.Interact) &&
            entity instanceof Player && !Friends.get().shouldAttack((Player) entity)) {
            return false;
        }

        // Babies
        if ((babies.get() == InteractMode.Both || babies.get() == InteractMode.Interact) &&
            entity instanceof Animal && ((Animal) entity).isBaby()) {
            return false;
        }

        // NameTagged
        if ((nametagged.get() == InteractMode.Both || nametagged.get() == InteractMode.Interact) && entity.hasCustomName())
            return false;

        // Entities
        if (entityInteractMode.get() == ListMode.BlackList &&
            entityInteract.get().contains(entity.getType())) {
            return false;
        } else return entityInteractMode.get() != ListMode.WhiteList ||
            entityInteract.get().contains(entity.getType());
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
