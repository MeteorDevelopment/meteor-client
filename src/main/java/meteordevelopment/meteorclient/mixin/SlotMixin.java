/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.ISlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// TODO(Ravel): can not resolve target class net.minecraft.world.inventory.Slot
// TODO(Ravel): can not resolve target class Slot
@Mixin(Slot.class)
public abstract class SlotMixin implements ISlot {
    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    public int id;
    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    @Final
    private int index;

    @Override
    public int meteor$getId() {
        return id;
    }

    @Override
    public int meteor$getIndex() {
        return index;
    }
}
