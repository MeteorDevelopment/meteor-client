/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.misc.EChestPreview;
import minegame159.meteorclient.utils.EChestMemory;
import minegame159.meteorclient.utils.KeyBinds;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {
    @Shadow @Nullable protected Slot focusedSlot;

    @Shadow protected int x;
    @Shadow protected int y;
    private static final Identifier TEXTURE = new Identifier("meteor-client", "container_3x9.png");
    private static MinecraftClient mc;

    public HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        mc = MinecraftClient.getInstance();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (focusedSlot != null && !focusedSlot.getStack().isEmpty()) {
            // Shulker Preview
            if (Utils.isShulker(focusedSlot.getStack().getItem()) && KeyBinds.SHULKER_PEEK.isPressed()) {
                CompoundTag compoundTag = focusedSlot.getStack().getSubTag("BlockEntityTag");
                if (compoundTag != null) {
                    if (compoundTag.contains("Items", 9)) {
                        DefaultedList<ItemStack> itemStacks = DefaultedList.ofSize(27, ItemStack.EMPTY);
                        Inventories.fromTag(compoundTag, itemStacks);

                        draw(matrices, itemStacks, mouseX, mouseY);
                    }
                }
            }

            // EChest preview
            if (focusedSlot.getStack().getItem() == Items.ENDER_CHEST && ModuleManager.INSTANCE.isActive(EChestPreview.class)) {
                draw(matrices, EChestMemory.ITEMS, mouseX, mouseY);
            }
        }
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void onDrawMouseoverTooltip(MatrixStack matrices, int x, int y, CallbackInfo info) {
        if (focusedSlot != null && !focusedSlot.getStack().isEmpty()) {
            if (Utils.isShulker(focusedSlot.getStack().getItem()) && KeyBinds.SHULKER_PEEK.isPressed()) info.cancel();
            else if (focusedSlot.getStack().getItem() == Items.ENDER_CHEST && ModuleManager.INSTANCE.isActive(EChestPreview.class)) info.cancel();
        }
    }

    private void draw(MatrixStack matrices, DefaultedList<ItemStack> itemStacks, int mouseX, int mouseY) {
        RenderSystem.disableLighting();
        RenderSystem.disableDepthTest();
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        drawBackground(matrices, mouseX + 6, mouseY + 6);
        DiffuseLighting.enable();

        int row = 0;
        int i = 0;
        for (ItemStack itemStack : itemStacks) {
            drawItem(itemStack, mouseX + 6 + 8 + i * 18, mouseY + 6 + 7 + row * 18);

            i++;
            if (i >= 9) {
                i = 0;
                row++;
            }
        }

        DiffuseLighting.disable();
        RenderSystem.enableDepthTest();
    }

    private void drawItem(ItemStack itemStack, int x, int y) {
        mc.getItemRenderer().renderGuiItemIcon(itemStack, x, y);
        mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, x, y, null);
    }

    private void drawBackground(MatrixStack matrices, int x, int y) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEXTURE);
        int width = 176;
        int height = 67;
        DrawableHelper.drawTexture(matrices, x, y, 0, 0, 0, width, height, height, width);
    }

}
