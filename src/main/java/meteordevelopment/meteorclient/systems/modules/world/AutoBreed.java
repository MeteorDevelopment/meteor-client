/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

public class AutoBreed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to breed.")
            .defaultValue(EntityType.HORSE, EntityType.DONKEY, EntityType.COW,
                    EntityType.MOOSHROOM, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN, EntityType.WOLF,
                    EntityType.CAT, EntityType.OCELOT, EntityType.RABBIT, EntityType.LLAMA, EntityType.TURTLE,
                    EntityType.PANDA, EntityType.FOX, EntityType.BEE, EntityType.STRIDER, EntityType.HOGLIN)
            .onlyAttackable()
            .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("How far away the animals can be to be bred.")
            .min(0)
            .defaultValue(4.5)
            .build()
    );

    private final Setting<Hand> hand = sgGeneral.add(new EnumSetting.Builder<Hand>()
            .name("hand-for-breeding")
            .description("The hand to use for breeding.")
            .defaultValue(Hand.MAIN_HAND)
            .build()
    );

    private final Setting<Boolean> ignoreBabies = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-babies")
            .description("Whether or not to ignore the baby variants of the specified entity.")
            .defaultValue(true)
            .build()
    );

    private final List<Entity> animalsFed = new ArrayList<>();

    public AutoBreed() {
        super(Categories.World, "auto-breed", "Automatically breeds specified animals.");
    }

    @Override
    public void onActivate() {
        animalsFed.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        for (Entity entity : mc.world.getEntities()) {
            AnimalEntity animal;

            if (!(entity instanceof AnimalEntity)) continue;
            else animal = (AnimalEntity) entity;

            if (!entities.get().getBoolean(animal.getType())
                    || (animal.isBaby() && !ignoreBabies.get())
                    || animalsFed.contains(animal)
                    || mc.player.distanceTo(animal) > range.get()
                    || !animal.isBreedingItem(hand.get() == Hand.MAIN_HAND ? mc.player.getMainHandStack() : mc.player.getOffHandStack())) continue;

            Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity), -100, () -> {
                mc.interactionManager.interactEntity(mc.player, animal, hand.get());
                mc.player.swingHand(hand.get());
                animalsFed.add(animal);
            });

            return;
        }
    }
}
