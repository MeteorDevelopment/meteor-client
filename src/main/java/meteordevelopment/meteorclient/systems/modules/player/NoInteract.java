/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;

import java.util.ArrayList;
import java.util.List;

public class NoInteract extends Module {
    private final SettingGroup sgBlocks = settings.createGroup("Blocks");
    private final SettingGroup sgAntiHit = settings.createGroup("Anti Hit");

    private final Setting<BlockInteractMode> blockInteract = sgBlocks.add(new EnumSetting.Builder<BlockInteractMode>()
        .name("block-interact")
        .description("Cancels block interaction.")
        .defaultValue(BlockInteractMode.None)
        .build()
    );

    private final Setting<Boolean> onlyCrystals = sgBlocks.add(new BoolSetting.Builder()
        .name("only-crystals")
        .description("Only blocks the interactions if the held item is an end crystal.")
        .defaultValue(false)
        .visible(() -> blockInteract.get() != BlockInteractMode.None)
        .build()
    );

    private final Setting<Hand> blockInteractHand = sgBlocks.add(new EnumSetting.Builder<Hand>()
        .name("block-interact-hand")
        .description("Only cancels block interaction if the hand used is this value.")
        .defaultValue(Hand.Mainhand)
        .visible(() -> blockInteract.get() != BlockInteractMode.None)
        .build()
    );

    private final Setting<List<Block>> blocks = sgBlocks.add(new BlockListSetting.Builder()
        .name("block-mining")
        .description("Will cancel mining these blocks.")
        .defaultValue(new ArrayList<>())
        .build()
    );

    // Anti Hit

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgAntiHit.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Will cancel hits on the entities selected.")
        .defaultValue(new Object2BooleanOpenHashMap<>(0))
        .onlyAttackable()
        .build()
    );

    private final Setting<Boolean> friends = sgAntiHit.add(new BoolSetting.Builder()
        .name("friends")
        .description("Cancels hits on players you have friended.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> babies = sgAntiHit.add(new BoolSetting.Builder()
        .name("babies")
        .description("Cancels hits on baby entities.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> nametagged = sgAntiHit.add(new BoolSetting.Builder()
        .name("nametagged")
        .description("Cancels hits on nametagged entities.")
        .defaultValue(false)
        .build()
    );

    public NoInteract() {
        super(Categories.Player, "no-interact", "Blocks interactions with certain types of inputs.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (blockInteract.get() == BlockInteractMode.Packet && event.packet instanceof PlayerInteractBlockC2SPacket) {
            switch (((PlayerInteractBlockC2SPacket) event.packet).getHand()) {
                case MAIN_HAND -> {
                    if ((blockInteractHand.get() == Hand.Mainhand || blockInteractHand.get() == Hand.Both)
                        && (!onlyCrystals.get() || mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL)) event.cancel();
                }
                case OFF_HAND -> {
                    if ((blockInteractHand.get() == Hand.Offhand || blockInteractHand.get() == Hand.Both)
                        && (!onlyCrystals.get() || mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL)) event.cancel();
                }
            }
        }
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        if (blockInteract.get() == BlockInteractMode.Normal) {
            switch (event.hand) {
                case MAIN_HAND -> {
                    if ((blockInteractHand.get() == Hand.Mainhand || blockInteractHand.get() == Hand.Both)
                        && (!onlyCrystals.get() || mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL)) event.cancel();
                }
                case OFF_HAND -> {
                    if ((blockInteractHand.get() == Hand.Offhand || blockInteractHand.get() == Hand.Both)
                        && (!onlyCrystals.get() || mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL)) event.cancel();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onAttackEntity(AttackEntityEvent event) {
        // Friends
        if (friends.get() && event.entity instanceof PlayerEntity && !Friends.get().shouldAttack((PlayerEntity) event.entity)) {
            event.cancel();
        }

        // Babies
        if (babies.get() && event.entity instanceof AnimalEntity && ((AnimalEntity) event.entity).isBaby()) {
            event.cancel();
        }

        // NameTagged
        if (nametagged.get() && event.entity.hasCustomName()) event.cancel();

        // Entities
        if (entities.get().getBoolean(event.entity.getType())) event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlockEvent(StartBreakingBlockEvent event) {
        if (blocks.get().contains(mc.world.getBlockState(event.blockPos).getBlock())) event.cancel();
    }

    public enum BlockInteractMode {
        Normal,
        Packet,
        None
    }

    public enum Hand {
        Mainhand,
        Offhand,
        Both
    }
}
