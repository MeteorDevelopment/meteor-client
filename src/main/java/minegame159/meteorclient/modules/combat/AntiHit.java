/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import minegame159.meteorclient.events.entity.player.AttackEntityEvent;
import minegame159.meteorclient.friends.Friends;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EntityTypeListSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;

public class AntiHit extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> antiFriendHit = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-friend-hit")
        .description("Doesn't allow friends to be attacked.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to avoid attacking.")
        .defaultValue(new Object2BooleanOpenHashMap<>(0))
        .onlyAttackable()
        .build()
    );

    public AntiHit() {
        super(Categories.Combat, "anti-hit", "Cancels out attacks on certain entities.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onAttackEntity(AttackEntityEvent event) {
        if (antiFriendHit.get() && event.entity instanceof PlayerEntity && !Friends.get().attack((PlayerEntity) event.entity)) event.cancel();
        if (Modules.get().isActive(AntiHit.class) && entities.get().containsKey(event.entity.getType())) event.cancel();
    }
}
