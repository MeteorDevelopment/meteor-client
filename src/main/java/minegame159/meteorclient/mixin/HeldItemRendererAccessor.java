/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HeldItemRenderer.class)
public interface HeldItemRendererAccessor {
    @Accessor("mainHand")
    void setItemStackMainHand(ItemStack value);

    @Accessor("offHand")
    void setItemStackOffHand(ItemStack value);

    @Accessor("equipProgressMainHand")
    void setEquippedProgressMainHand(float value);

    @Accessor("equipProgressOffHand")
    void setEquippedProgressOffHand(float value);
}