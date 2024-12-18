/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.Iterator;
import java.util.Set;

public class AutoNametag extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Which entities to nametag.")
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The maximum range an entity can be to be nametagged.")
        .defaultValue(5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("priority")
        .description("Priority sort")
        .defaultValue(SortPriority.LowestDistance)
        .build()
    );

    private final Setting<Boolean> renametag = sgGeneral.add(new BoolSetting.Builder()
        .name("renametag")
        .description("Allows already nametagged entities to be renamed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically faces towards the mob being nametagged.")
        .defaultValue(true)
        .build()
    );

    private final Object2IntMap<Entity> entityCooldowns = new Object2IntOpenHashMap<>();

    private Entity target;
    private boolean offHand;

    public AutoNametag() {
        super(Categories.World, "auto-nametag", "Automatically uses nametags on entities without a nametag. WILL nametag ALL entities in the specified distance.");
    }

    @Override
    public void onDeactivate() {
        entityCooldowns.clear();
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        // Find nametag in hotbar
        FindItemResult findNametag = InvUtils.findInHotbar(Items.NAME_TAG);

        if (!findNametag.found()) {
            error("No Nametag in Hotbar");
            toggle();
            return;
        }

        // Target
        target = TargetUtils.get(entity -> {
            if (!PlayerUtils.isWithin(entity, range.get())) return false;
            if (!entities.get().contains(entity.getType())) return false;

            if (entity.hasCustomName() && (!renametag.get() || entity.getCustomName().equals(mc.player.getInventory().getStack(findNametag.slot()).getName())))
                return false;

            return entityCooldowns.getInt(entity) <= 0;
        }, priority.get());

        if (target == null)
            return;

        // Swapping slots
        InvUtils.swap(findNametag.slot(), true);

        offHand = findNametag.isOffhand();

        // Interaction
        if (rotate.get()) Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), -100, this::interact);
        else interact();
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        for (Iterator<Entity> it = entityCooldowns.keySet().iterator(); it.hasNext(); ) {
            Entity entity = it.next();
            int cooldown = entityCooldowns.getInt(entity) - 1;

            if (cooldown <= 0) it.remove();
            else entityCooldowns.put(entity, cooldown);
        }
    }

    private void interact() {
        mc.interactionManager.interactEntity(mc.player, target, offHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
        InvUtils.swapBack();

        entityCooldowns.put(target, 20);
    }
}
