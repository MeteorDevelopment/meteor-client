/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
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
import meteordevelopment.meteorclient.utils.misc.FilterMode;
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

    private final Setting<FilterMode> blockMineMode = sgBlocks.add(new EnumSetting.Builder<FilterMode>()
        .name("block-mine-mode")
        .description("Filter mode to use for block mine.")
        .defaultValue(FilterMode.Blacklist)
        .build()
    );

    private final Setting<List<Block>> mineBlocks = sgBlocks.add(new BlockListSetting.Builder()
        .name("block-mine")
        .description("Cancels mining selected blocks.")
        .visible(() -> !blockMineMode.get().isWildCard())
        .build()
    );

    private final Setting<FilterMode> blockInteractMode = sgBlocks.add(new EnumSetting.Builder<FilterMode>()
        .name("block-interact-mode")
        .description("Filter mode to use for block interact.")
        .defaultValue(FilterMode.Blacklist)
        .build()
    );

    private final Setting<List<Block>> interactBlocks = sgBlocks.add(new BlockListSetting.Builder()
        .name("block-interact")
        .description("Cancels interaction for selected blocks.")
        .visible(() -> !blockInteractMode.get().isWildCard())
        .build()
    );

    private final Setting<HandMode> blockInteractHand = sgBlocks.add(new EnumSetting.Builder<HandMode>()
        .name("block-interact-hand")
        .description("Cancels block interaction if performed by this hand.")
        .defaultValue(HandMode.None)
        .build()
    );

    // Entities

    private final Setting<FilterMode> entityHitMode = sgEntities.add(new EnumSetting.Builder<FilterMode>()
        .name("entity-hit-mode")
        .description("Filter mode to use for entity hit.")
        .defaultValue(FilterMode.Blacklist)
        .build()
    );

    private final Setting<Object2BooleanMap<EntityType<?>>> hitEntities = sgEntities.add(new EntityTypeListSetting.Builder()
        .name("entity-hit")
        .description("Cancel hitting selected entities.")
        .onlyAttackable()
        .visible(() -> !entityHitMode.get().isWildCard())
        .build()
    );


    private final Setting<FilterMode> entityInteractMode = sgEntities.add(new EnumSetting.Builder<FilterMode>()
        .name("entity-interact-mode")
        .description("List mode to use for entity interact.")
        .defaultValue(FilterMode.Blacklist)
        .build()
    );

    private final Setting<Object2BooleanMap<EntityType<?>>> interactEntities = sgEntities.add(new EntityTypeListSetting.Builder()
        .name("entity-interact")
        .description("Cancel interaction with selected entities.")
        .onlyAttackable()
        .visible(() -> !entityInteractMode.get().isWildCard())
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
        return !blockMineMode.get().test(mineBlocks.get(), mc.world.getBlockState(blockPos).getBlock());
    }

    private boolean shouldInteractBlock(BlockHitResult hitResult, Hand hand) {
        // Hand Interactions
        if (blockInteractHand.get() == HandMode.Both ||
            (blockInteractHand.get() == HandMode.Mainhand && hand == Hand.MAIN_HAND) ||
            (blockInteractHand.get() == HandMode.Offhand && hand == Hand.OFF_HAND)) {
            return false;
        }

        // Blocks
        return !blockInteractMode.get().test(interactBlocks.get(), mc.world.getBlockState(hitResult.getBlockPos()).getBlock());
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
        return !entityHitMode.get().test(hitEntities.get(), entity);
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
        return !entityInteractMode.get().test(interactEntities.get(), entity);
    }

    public enum HandMode {
        Mainhand,
        Offhand,
        Both,
        None
    }

    public enum InteractMode {
        Hit,
        Interact,
        Both,
        None
    }
}
