/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;

public class Trigger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();



    private final Setting<Boolean> whenHoldingLeftClick = sgGeneral.add(new BoolSetting.Builder()
            .name("when-holding-left-click")
            .description("Attacks only when you are holding left click.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Weapons> weapon = sgGeneral.add(new EnumSetting.Builder<Weapons>()
            .name("weapon")
            .description("Only attacks an entity when a specified weapon is in your hand.")
            .defaultValue(Weapons.Any)
            .build()
    );

    public Trigger() {
        super(Categories.Combat, "trigger", "Automatically swings when you look at entities.");
    }

    private Entity target;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        target = null;

        //Make sure that the player is not dead and that the entity is still alive
        if (mc.player.getHealth() <= 0 || mc.player.getAttackCooldownProgress(0.5f) < 1) return;
        if (!(mc.targetedEntity instanceof LivingEntity)) return;
        if (((LivingEntity) mc.targetedEntity).getHealth() <= 0) return;

        target = mc.targetedEntity;

        //Checks if the player is holding the specified weapon
        if(
                weapon.get() == Weapons.Axe && mc.player.getMainHandStack().getItem() instanceof AxeItem ||
                weapon.get() == Weapons.Sword && mc.player.getMainHandStack().getItem() instanceof SwordItem ||
                weapon.get() == Weapons.Both && mc.player.getMainHandStack().getItem() instanceof AxeItem ||
                weapon.get() == Weapons.Both && mc.player.getMainHandStack().getItem() instanceof SwordItem ||
                weapon.get() == Weapons.Any
        ) {
            //Attack the entity
            if (whenHoldingLeftClick.get()) {
                if (mc.options.keyAttack.isPressed()) attack(target);
            } else {
                attack(target);
            }

        }
    }

    private void attack(Entity entity) {
        //Attack the entity
        mc.interactionManager.attackEntity(mc.player, entity);
        mc.player.swingHand(Hand.MAIN_HAND);
    }



    @Override
    public String getInfoString() {
        if (target != null && target instanceof PlayerEntity) return target.getEntityName();
        if (target != null) return target.getType().getName().getString();
        return null;
    }
    
    public enum Weapons {
        Sword,
        Axe,
        Both,
        Any
    }
    
}
