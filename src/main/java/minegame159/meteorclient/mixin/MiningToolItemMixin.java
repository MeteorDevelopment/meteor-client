/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IMiningToolItem;
import net.minecraft.block.Block;
import net.minecraft.item.MiningToolItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(MiningToolItem.class)
public class MiningToolItemMixin implements IMiningToolItem {
    @Shadow @Final private Set<Block> effectiveBlocks;

    @Override
    public boolean isEffectiveOn(Block block) {
        return effectiveBlocks.contains(block);
    }
}
