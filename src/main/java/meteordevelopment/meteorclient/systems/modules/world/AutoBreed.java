/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.Hand;

import java.util.*;

public class AutoBreed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
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

    private final Setting<EntityAge> mobAgeFilter = sgGeneral.add(new EnumSetting.Builder<EntityAge>()
        .name("mob-age-filter")
        .description("Determines the age of the mobs to target (baby, adult, or both).")
        .defaultValue(EntityAge.Adult)
        .build()
    );

    private final Setting<Boolean> continuousBreeding = sgGeneral.add(new BoolSetting.Builder()
        .name("continuous-breeding")
        .description("Whether to feed the same animal again after a certain time period.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> breedingInterval = sgGeneral.add(new IntSetting.Builder()
        .name("breeding-interval")
        .description("Determines how often the same animal is fed in ticks.")
        .min(1)
        .sliderMax(24000)
        .defaultValue(6600) // 30s in love mode and the 5-minute breeding cooldown
        .visible(continuousBreeding::get)
        .build()
    );

    private final LinkedHashMap<Entity, Integer> animalsFed = new LinkedHashMap<>();
    private int tickCounter = 0;

    public AutoBreed() {
        super(Categories.World, "auto-breed", "Automatically breeds specified animals.");
    }

    @Override
    public void onActivate() {
        animalsFed.clear();
        tickCounter = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof AnimalEntity animal)) continue;

            if (!entities.get().contains(animal.getType())
                || !isCorrectAge(animal)
                || animalsFed.containsKey(animal)
                || !PlayerUtils.isWithin(animal, range.get())
                || !animal.isBreedingItem(hand.get() == Hand.MAIN_HAND ? mc.player.getMainHandStack() : mc.player.getOffHandStack()))
                continue;

            Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity), -100, () -> {
                mc.interactionManager.interactEntity(mc.player, animal, hand.get());
                mc.player.swingHand(hand.get());
                animalsFed.putLast(animal, tickCounter);
            });
            break;
        }

        if (continuousBreeding.get()) {
            while (!animalsFed.isEmpty() && animalsFed.firstEntry().getValue() < tickCounter - breedingInterval.get()) {
                animalsFed.pollFirstEntry();
            }
            tickCounter++;
        }
    }

    public enum EntityAge {
        Baby,
        Adult,
        Both
    }

    private boolean isCorrectAge(AnimalEntity animal) {
        return switch (mobAgeFilter.get()) {
            case Baby -> animal.isBaby();
            case Adult -> !animal.isBaby();
            case Both -> true;
        };
    }
}
