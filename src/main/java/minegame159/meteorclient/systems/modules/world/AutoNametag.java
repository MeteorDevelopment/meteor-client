/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.world;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.player.Rotations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.NameTagItem;
import net.minecraft.util.Hand;

public class AutoNametag extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Which entities to nametag.")
            .defaultValue(new Object2BooleanOpenHashMap<>(0))
            .build()
    );
    
    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
            .name("distance")
            .description("The maximum distance a nametagged entity can be to be nametagged.")
            .min(0.0)
            .defaultValue(5.0)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically faces towards the mob being nametagged.")
            .defaultValue(true)
            .build()
    );

    private Entity entity;
    private boolean offHand;
    private int preSlot;

    public AutoNametag() {
        super(Categories.World, "auto-nametag", "Automatically uses nametags on entities without a nametag. WILL nametag ALL entities in the specified distance.");
    }

    @Override
    public void onDeactivate() {
        entity = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        entity = null;

        for (Entity entity : mc.world.getEntities()) {
            if (!entities.get().getBoolean(entity.getType()) || entity.hasCustomName() || mc.player.distanceTo(entity) > distance.get()) continue;

            boolean findNametag = true;
            if (mc.player.inventory.getMainHandStack().getItem() instanceof NameTagItem) {
                findNametag = false;
            }
            else if (mc.player.inventory.offHand.get(0).getItem() instanceof NameTagItem) {
                findNametag = false;
                offHand = true;
            }

            boolean foundNametag = !findNametag;
            if (findNametag) {
                for (int i = 0; i < 9; i++) {
                    ItemStack itemStack = mc.player.inventory.getStack(i);
                    if (itemStack.getItem() instanceof NameTagItem) {
                        preSlot = mc.player.inventory.selectedSlot;
                        mc.player.inventory.selectedSlot = i;
                        foundNametag = true;
                        break;
                    }
                }
            }

            if (foundNametag) {
                this.entity = entity;

                if (rotate.get()) Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity), -100, this::interact);
                else interact();

                return;
            }
        }
    }

    private void interact() {
        mc.interactionManager.interactEntity(mc.player, entity, offHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
        mc.player.inventory.selectedSlot = preSlot;
    }
}
