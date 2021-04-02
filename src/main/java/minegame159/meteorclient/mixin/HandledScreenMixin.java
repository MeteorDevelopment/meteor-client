/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.misc.MapPreview;
import minegame159.meteorclient.modules.render.EChestPreview;
import minegame159.meteorclient.modules.render.ItemHighlight;
import minegame159.meteorclient.modules.render.ShulkerPeek;
import minegame159.meteorclient.utils.player.EChestMemory;
import minegame159.meteorclient.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
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
    private static final Identifier LIGHT = new Identifier("meteor-client", "container_3x9.png");
    private static final Identifier DARK = new Identifier("meteor-client", "container_3x9-dark.png");
    private static final Identifier TEXTURE_MAP_BACKGROUND = new Identifier("textures/map/map_background.png");
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
            ShulkerPeek shulkerPeek = Modules.get().get(ShulkerPeek.class);

            if (shulkerPeek.isActive() && ((shulkerPeek.isPressed() && shulkerPeek.mode.get() == ShulkerPeek.Mode.Tooltip) || (shulkerPeek.mode.get() == ShulkerPeek.Mode.Always))) {
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
            if (focusedSlot.getStack().getItem() == Items.ENDER_CHEST && Modules.get().isActive(EChestPreview.class)) {
                draw(matrices, EChestMemory.ITEMS, mouseX, mouseY);
            }

            // Map preview
            if (focusedSlot.getStack().getItem() == Items.FILLED_MAP && Modules.get().isActive(MapPreview.class)) {
                drawMapPreview(matrices, focusedSlot.getStack(), mouseX, mouseY, Modules.get().get(MapPreview.class).getScale());
            }
        }
    }

    private boolean hasItems(ItemStack itemStack) {
        CompoundTag compoundTag = itemStack.getSubTag("BlockEntityTag");
        return compoundTag != null && compoundTag.contains("Items", 9);
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void onDrawMouseoverTooltip(MatrixStack matrices, int x, int y, CallbackInfo info) {
        if (focusedSlot != null && !focusedSlot.getStack().isEmpty()) {
            ShulkerPeek shulkerPeek = Modules.get().get(ShulkerPeek.class);
            if (Modules.get().isActive(ShulkerPeek.class) && hasItems(focusedSlot.getStack()) && ((shulkerPeek.isPressed() && Modules.get().get(ShulkerPeek.class).mode.get() == ShulkerPeek.Mode.Tooltip) || (Modules.get().get(ShulkerPeek.class).mode.get() == ShulkerPeek.Mode.Always))) info.cancel();
            else if (focusedSlot.getStack().getItem() == Items.ENDER_CHEST && Modules.get().isActive(EChestPreview.class)) info.cancel();
            else if (focusedSlot.getStack().getItem() == Items.FILLED_MAP && Modules.get().isActive(MapPreview.class)) info.cancel();
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
            RenderUtils.drawItem(itemStack, mouseX + 6 + 8 + i * 18, mouseY + 6 + 7 + row * 18, true);

            i++;
            if (i >= 9) {
                i = 0;
                row++;
            }
        }

        DiffuseLighting.disable();
        RenderSystem.enableDepthTest();
    }

    private void drawBackground(MatrixStack matrices, int x, int y) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(Modules.get().get(ShulkerPeek.class).bgMode.get() == ShulkerPeek.BackgroundMode.Light ? LIGHT : DARK);
        int width = 176;
        int height = 67;
        DrawableHelper.drawTexture(matrices, x, y, 0, 0, 0, width, height, height, width);
    }

    private void drawMapPreview(MatrixStack matrices, ItemStack stack, int x, int y, int dimensions)
    {
        GL11.glEnable(GL11.GL_BLEND);
        RenderSystem.pushMatrix();
        RenderSystem.disableLighting();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        int y1 = y + 8;
        int y2 = y1 + dimensions;
        int x1 = x + 8;
        int x2 = x1 + dimensions;
        int z = 300;

        mc.getTextureManager().bindTexture(TEXTURE_MAP_BACKGROUND);

        //DrawableHelper.drawTexture(matrices, x1, y1, x2, y2, 64, 64);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(x1, y2, z).texture(0.0f, 1.0f).next();
        buffer.vertex(x2, y2, z).texture(1.0f, 1.0f).next();
        buffer.vertex(x2, y1, z).texture(1.0f, 0.0f).next();
        buffer.vertex(x1, y1, z).texture(0.0f, 0.0f).next();
        tessellator.draw();

        MapState mapState = FilledMapItem.getMapState(stack, mc.world);

        if (mapState != null)
        {
            mapState.getPlayerSyncData(mc.player);

            x1 += 8;
            y1 += 8;
            z = 310;
            VertexConsumerProvider.Immediate consumer = mc.getBufferBuilders().getEntityVertexConsumers();
            double scale = (double) (dimensions - 16) / 128.0D;
            RenderSystem.translatef(x1, y1, z);
            RenderSystem.scaled(scale, scale, 0);
            mc.gameRenderer.getMapRenderer().draw(matrices, consumer, mapState, false, 0xF000F0);
        }

        RenderSystem.enableLighting();
        RenderSystem.popMatrix();
    }

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void onDrawSlot(MatrixStack matrices, Slot slot, CallbackInfo info) {
        int color = Modules.get().get(ItemHighlight.class).getColor(slot.getStack());
        if (color != -1) fill(matrices, slot.x, slot.y, slot.x + 16, slot.y + 16, color);
    }
}
