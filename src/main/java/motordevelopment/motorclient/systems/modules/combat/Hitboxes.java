/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.modules.combat;

import motordevelopment.motorclient.settings.*;
import motordevelopment.motorclient.systems.friends.Friends;
import motordevelopment.motorclient.systems.modules.Categories;
import motordevelopment.motorclient.systems.modules.Module;
import motordevelopment.motorclient.utils.player.InvUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;

import java.util.Set;

public class Hitboxes extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

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

    private final Setting<Boolean> onlyOnWeapon = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-weapon")
        .description("Only modifies hitbox when holding a weapon in hand.")
        .defaultValue(false)
        .build()
    );

    public Hitboxes() {
        super(Categories.Combat, "hitboxes", "Expands an entity's hitboxes.");
    }

    public double getEntityValue(Entity entity) {
        if (!(isActive() && testWeapon()) || (ignoreFriends.get() && entity instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) entity))) return 0;
        if (entities.get().contains(entity.getType())) return value.get();
        return 0;
    }

    private boolean testWeapon() {
        if (!onlyOnWeapon.get()) return true;
        return InvUtils.testInHands(itemStack -> itemStack.getItem() instanceof SwordItem || itemStack.getItem() instanceof AxeItem);
    }
}
