/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.render.BetterTooltips;
import minegame159.meteorclient.systems.modules.render.ItemHighlight;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.player.EChestMemory;
import minegame159.meteorclient.utils.render.RenderUtils;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

import static minegame159.meteorclient.systems.commands.commands.PeekCommand.PeekShulkerBoxScreen;
import static minegame159.meteorclient.systems.modules.render.BetterTooltips.hasItems;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {
    @Shadow @Nullable protected Slot focusedSlot;

    @Shadow protected int x;
    @Shadow protected int y;

    private static final Identifier TEXTURE_CONTAINER_BACKGROUND = new Identifier("meteor-client", "container_3x9.png");
    private static final Identifier TEXTURE_MAP_BACKGROUND = new Identifier("textures/map/map_background.png");

    private static final ItemStack[] ITEMS = new ItemStack[27];

    public HandledScreenMixin(Text title) {
        super(title);
    }

    // Better Tooltips

    // Middleclick open
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && focusedSlot != null && !focusedSlot.getStack().isEmpty()) {
            BetterTooltips toolips = Modules.get().get(BetterTooltips.class);

            if (hasItems(focusedSlot.getStack()) && toolips.middleClickOpen.get()) {
                Utils.getItemsInContainerItem(focusedSlot.getStack(), ITEMS);
                client.openScreen(new PeekShulkerBoxScreen(new ShulkerBoxScreenHandler(0, client.player.inventory, new SimpleInventory(ITEMS)), client.player.inventory, focusedSlot.getStack().getName()));
                cir.setReturnValue(true);
            } else if (focusedSlot.getStack().getItem() == Items.ENDER_CHEST && toolips.previewEChest()) {
                for (int i = 0; i < EChestMemory.ITEMS.size(); i++) ITEMS[i] = EChestMemory.ITEMS.get(i);
                client.openScreen(new PeekShulkerBoxScreen(new ShulkerBoxScreenHandler(0, client.player.inventory, new SimpleInventory(ITEMS)), client.player.inventory, focusedSlot.getStack().getName()));
                cir.setReturnValue(true);
            }
        }
    }

    //Rendering previews
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (focusedSlot != null && !focusedSlot.getStack().isEmpty()) {
            BetterTooltips toolips = Modules.get().get(BetterTooltips.class);

            // Shulker Preview
            if (hasItems(focusedSlot.getStack()) && toolips.previewShulkers()) {
                CompoundTag compoundTag = focusedSlot.getStack().getSubTag("BlockEntityTag");
                DefaultedList<ItemStack> itemStacks = DefaultedList.ofSize(27, ItemStack.EMPTY);
                Inventories.fromTag(compoundTag, itemStacks);
                draw(matrices, itemStacks, mouseX, mouseY, toolips.getShulkerColor(focusedSlot.getStack()));
            }

            // EChest preview
            else if (focusedSlot.getStack().getItem() == Items.ENDER_CHEST && toolips.previewEChest()) {
                draw(matrices, EChestMemory.ITEMS, mouseX, mouseY, toolips.echestColor.get());
            }

            // Map preview
            else if (focusedSlot.getStack().getItem() == Items.FILLED_MAP && toolips.previewMaps()) {
                drawMapPreview(matrices, focusedSlot.getStack(), mouseX, mouseY, toolips.mapsScale.get());
            }
        }
    }

    // Hiding vanilla tooltips
    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void onDrawMouseoverTooltip(MatrixStack matrices, int x, int y, CallbackInfo info) {
        if (focusedSlot != null && !focusedSlot.getStack().isEmpty()) {
            BetterTooltips toolips = Modules.get().get(BetterTooltips.class);

            if (focusedSlot.getStack().getItem() == Items.FILLED_MAP && toolips.previewMaps()) info.cancel();
            else if ((hasItems(focusedSlot.getStack())
                    && toolips.previewShulkers()
                    || (focusedSlot.getStack().getItem() == Items.ENDER_CHEST
                    && toolips.previewEChest()))
                    && !toolips.showVanilla.get()) info.cancel();
        }
    }

    private void draw(MatrixStack matrices, DefaultedList<ItemStack> itemStacks, int mouseX, int mouseY, Color color) {
        RenderSystem.disableLighting();
        RenderSystem.disableDepthTest();
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        mouseX += 8;
        mouseY -= 12;

        drawBackground(matrices, mouseX, mouseY, color);

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        DiffuseLighting.enable();

        int row = 0;
        int i = 0;
        for (ItemStack itemStack : itemStacks) {
            RenderUtils.drawItem(itemStack, mouseX + 8 + i * 18, mouseY + 7 + row * 18, true);

            i++;
            if (i >= 9) {
                i = 0;
                row++;
            }
        }

        DiffuseLighting.disable();
        RenderSystem.enableDepthTest();
    }

    private void drawBackground(MatrixStack matrices, int x, int y, Color color) {
        RenderSystem.color4f(color.r / 255F, color.g / 255F, color.b / 255F, color.a / 255F);
        client.getTextureManager().bindTexture(TEXTURE_CONTAINER_BACKGROUND);
        DrawableHelper.drawTexture(matrices, x, y, 0, 0, 0, 176, 67, 67, 176);
    }

    private void drawMapPreview(MatrixStack matrices, ItemStack stack, int x, int y, int dimensions) {
        GL11.glEnable(GL11.GL_BLEND);
        RenderSystem.pushMatrix();
        RenderSystem.disableLighting();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        int y1 = y - 12;
        int y2 = y1 + dimensions;
        int x1 = x + 8;
        int x2 = x1 + dimensions;
        int z = 300;

        client.getTextureManager().bindTexture(TEXTURE_MAP_BACKGROUND);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(x1, y2, z).texture(0.0f, 1.0f).next();
        buffer.vertex(x2, y2, z).texture(1.0f, 1.0f).next();
        buffer.vertex(x2, y1, z).texture(1.0f, 0.0f).next();
        buffer.vertex(x1, y1, z).texture(0.0f, 0.0f).next();
        tessellator.draw();

        MapState mapState = FilledMapItem.getOrCreateMapState(stack, client.world);

        if (mapState != null) {
            mapState.getPlayerSyncData(client.player);

            x1 += 8;
            y1 += 8;
            z = 310;
            double scale = (double) (dimensions - 16) / 128.0D;

            RenderSystem.translatef(x1, y1, z);
            RenderSystem.scaled(scale, scale, 0);
            VertexConsumerProvider.Immediate consumer = client.getBufferBuilders().getEntityVertexConsumers();
            client.gameRenderer.getMapRenderer().draw(matrices, consumer, mapState, false, 0xF000F0);
        }

        RenderSystem.enableLighting();
        RenderSystem.popMatrix();
    }

    // Item Highlight
    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void onDrawSlot(MatrixStack matrices, Slot slot, CallbackInfo info) {
        int color = Modules.get().get(ItemHighlight.class).getColor(slot.getStack());
        if (color != -1) fill(matrices, slot.x, slot.y, slot.x + 16, slot.y + 16, color);
    }
}
