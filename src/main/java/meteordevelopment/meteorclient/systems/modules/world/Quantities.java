/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import java.util.List;
import java.util.Set;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;

public class Quantities extends Module
{
    private final SettingGroup sgItems = settings.createGroup("Items");

    private final Setting<List<Item>> items = sgItems.add(new ItemListSetting.Builder()
        .name("items")
        .description("Select specific items.")
        .build()
    );

    private final Setting<Boolean> increment = sgItems.add(new BoolSetting.Builder()
        .name("increment")
        .description("Disables/enables increment.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> decrement = sgItems.add(new BoolSetting.Builder()
        .name("decrement")
        .description("Disables/enables decrement.")
        .defaultValue(false)
        .build()
    );

    public Quantities()
    {
        super(Categories.World, "quantities", "Control quantity to certain items/entities.");
    }


    public boolean inItemsList(Item item)
    {
        return isActive() && items.get().contains(item);
    }

    public boolean incr(Item item)
    {
        return inItemsList(item) && increment.get();
    }

    public boolean decr(Item item)
    {
        return inItemsList(item) && decrement.get();
    }
}
