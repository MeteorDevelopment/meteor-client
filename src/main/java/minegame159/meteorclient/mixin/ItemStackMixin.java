/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.misc.ItemByteSize;
import minegame159.meteorclient.utils.ByteCountDataOutput;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow public abstract CompoundTag toTag(CompoundTag tag);

    @Shadow private int count;

    @Inject(method = "getTooltip", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onGetTooltip(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> info, List<Text> list) {
        if (player != null && ModuleManager.INSTANCE.isActive(ItemByteSize.class)) {
            try {
                toTag(new CompoundTag()).write(ByteCountDataOutput.INSTANCE);
                int byteCount = ByteCountDataOutput.INSTANCE.getCount();
                ByteCountDataOutput.INSTANCE.reset();

                list.add(new LiteralText(Formatting.GRAY + ModuleManager.INSTANCE.get(ItemByteSize.class).bytesToString(byteCount)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
