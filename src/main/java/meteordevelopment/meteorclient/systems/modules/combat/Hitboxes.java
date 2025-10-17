/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.tag.ItemTags;

import java.util.Set;

public class Hitboxes extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWeapon = settings.createGroup("Weapon Options");

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Which entities to target.")
        .defaultValue(EntityType.PLAYER)
        .build()
    );

    private final Setting<Double> value = sgGeneral.add(new DoubleSetting.Builder()
        .name("expand")
        .description("How much to expand the hitbox of the entity.")
        .defaultValue(0.5)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Doesn't expand the hitboxes of friends.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyOnWeapon = sgWeapon.add(new BoolSetting.Builder()
        .name("only-on-weapon")
        .description("Only modifies hitbox when holding a weapon in hand.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> sword = sgWeapon.add(new BoolSetting.Builder()
        .name("sword")
        .description("Enable when holding a sword.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> axe = sgWeapon.add(new BoolSetting.Builder()
        .name("axe")
        .description("Enable when holding an axe.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> pickaxe = sgWeapon.add(new BoolSetting.Builder()
        .name("pickaxe")
        .description("Enable when holding a pickaxe.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> shovel = sgWeapon.add(new BoolSetting.Builder()
        .name("shovel")
        .description("Enable when holding a shovel.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> hoe = sgWeapon.add(new BoolSetting.Builder()
        .name("hoe")
        .description("Enable when holding a hoe.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> mace = sgWeapon.add(new BoolSetting.Builder()
        .name("mace")
        .description("Enable when holding a mace.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    public Hitboxes() {
        super(Categories.Combat, "hitboxes", "Expands an entity's hitboxes.");
    }

    public double getEntityValue(Entity entity) {
        if (!(isActive() && testWeapon()) || (ignoreFriends.get() && entity instanceof PlayerEntity playerEntity && Friends.get().isFriend(playerEntity))) return 0;
        if (entities.get().contains(entity.getType())) return value.get();
        return 0;
    }

    private boolean testWeapon() {
        if (!onlyOnWeapon.get()) return true;
        return InvUtils.testInMainHand(itemStack -> {
            if (sword.get() && itemStack.isIn(ItemTags.SWORDS)) return true;
            if (axe.get() && itemStack.isIn(ItemTags.AXES)) return true;
            if (pickaxe.get() && itemStack.isIn(ItemTags.PICKAXES)) return true;
            if (shovel.get() && itemStack.isIn(ItemTags.SHOVELS)) return true;
            if (hoe.get() && itemStack.isIn(ItemTags.HOES)) return true;
            if (mace.get() && itemStack.getItem() instanceof MaceItem) return true;
            return false;
        });
    }
}
