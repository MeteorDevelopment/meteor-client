/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import motordevelopment.motorclient.mixininterface.IMiningToolItem;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MiningToolItem.class)
public abstract class MiningToolItemMixin implements IMiningToolItem {
    @Unique
    private ToolMaterial material;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(ToolMaterial material, TagKey<Block> effectiveBlocks, float attackDamage, float attackSpeed, Item.Settings settings, CallbackInfo info) {
        this.material = material;
    }

    @Override
    public ToolMaterial motor$getMaterial() {
        return material;
    }
}
