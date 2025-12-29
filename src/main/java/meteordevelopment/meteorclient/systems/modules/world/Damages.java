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

public class Damages extends Module
{
    private final SettingGroup sgEntities = settings.createGroup("Entities");
    private final SettingGroup sgItems = settings.createGroup("Items");

    public final Setting<Set<EntityType<?>>> entities = sgEntities.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Select specific entities.")
        .build()
    );

    private final Setting<List<Item>> items = sgItems.add(new ItemListSetting.Builder()
        .name("items")
        .description("Select specific items.")
        .build()
    );

    public Damages()
    {
        super(Categories.World, "damages", "Removes damage to certain items/entities.");
    }

    public boolean inEntityList(Entity entity)
    {
        return isActive() && entities.get().contains(entity.getType());
    }

    public boolean inItemsList(Item item)
    {
        return isActive() && items.get().contains(item);
    }
}
