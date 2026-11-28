/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity;

import com.google.common.base.Predicates;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.NullMarked;

import java.util.function.Predicate;

@NullMarked
public enum EntityAgeTest implements Predicate<LivingEntity> {
    Baby,
    Adult,
    Both,
    ;

    @Override
    public boolean test(LivingEntity livingEntity) {
        return switch (this) {
            case Baby -> livingEntity.isBaby();
            case Adult -> !livingEntity.isBaby();
            case Both -> true;
        };
    }

    @Override
    public Predicate<LivingEntity> negate() {
        return switch (this) {
            case Baby -> Adult;
            case Adult -> Baby;
            case Both -> Predicates.alwaysFalse();
        };
    }

}
