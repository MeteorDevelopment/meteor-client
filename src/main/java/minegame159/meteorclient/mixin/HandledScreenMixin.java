/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.misc.InventoryTweaks;
import minegame159.meteorclient.systems.modules.render.BetterTooltips;
import minegame159.meteorclient.systems.modules.render.ItemHighlight;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.player.EChestMemory;
import minegame159.meteorclient.utils.render.RenderUtils;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
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

import static minegame159.meteorclient.utils.Utils.mc;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {
    @Shadow protected Slot focusedSlot;

    @Shadow protected int x;
    @Shadow protected int y;

    @Shadow @org.jetbrains.annotations.Nullable protected abstract Slot getSlotAt(double xPosition, double yPosition);

    @Shadow public abstract T getScreenHandler();

    @Shadow private boolean doubleClicking;

    @Shadow protected abstract void onMouseClick(Slot slot, int invSlot, int clickData, SlotActionType actionType);

    private static final Identifier TEXTURE_CONTAINER_BACKGROUND = new Identifier("meteor-client", "textures/container.png");
    private static final Identifier TEXTURE_MAP_BACKGROUND = new Identifier("textures/map/map_background.png");

    private static final ItemStack[] ITEMS = new ItemStack[27];

    public HandledScreenMixin(Text title) {
        super(title);
    }

    // Inventory Tweaks
    @Inject(method = "mouseDragged", at = @At("TAIL"))
    private void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> info) {
        if (button != GLFW_MOUSE_BUTTON_LEFT || doubleClicking || !Modules.get().get(InventoryTweaks.class).mouseDragItemMove()) return;

        Slot slot = getSlotAt(mouseX, mouseY);
        if (slot != null && slot.hasStack() && hasShiftDown()) onMouseClick(slot, slot.id, button, SlotActionType.QUICK_MOVE);
    }

    // Better Tooltips

    // Middle click open
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        BetterTooltips toolips = Modules.get().get(BetterTooltips.class);

        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && focusedSlot != null && !focusedSlot.getStack().isEmpty() && mc.player.currentScreenHandler.getCursorStack().isEmpty() && toolips.middleClickOpen()) {
            ItemStack itemStack = focusedSlot.getStack();
            if (Utils.hasItems(itemStack) || itemStack.getItem() == Items.ENDER_CHEST) {
                cir.setReturnValue(Utils.openContainer(focusedSlot.getStack(), ITEMS, false));
            }
        }
    }

    //Rendering previews
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (focusedSlot != null && !focusedSlot.getStack().isEmpty() && mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            BetterTooltips toolips = Modules.get().get(BetterTooltips.class);

            // Shulker Preview
            if (Utils.hasItems(focusedSlot.getStack()) && toolips.previewShulkers()) {
                NbtCompound compoundTag = focusedSlot.getStack().getSubTag("BlockEntityTag");
                DefaultedList<ItemStack> itemStacks = DefaultedList.ofSize(27, ItemStack.EMPTY);
                Inventories.readNbt(compoundTag, itemStacks);
                draw(matrices, itemStacks, mouseX, mouseY, Utils.getShulkerColor(focusedSlot.getStack()));
            }

            // EChest preview
            else if (focusedSlot.getStack().getItem() == Items.ENDER_CHEST && toolips.previewEChest()) {
                draw(matrices, EChestMemory.ITEMS, mouseX, mouseY, BetterTooltips.ECHEST_COLOR);
            }

            // Map preview
            else if (focusedSlot.getStack().getItem() == Items.FILLED_MAP && toolips.previewMaps()) {
                drawMapPreview(matrices, focusedSlot.getStack(), mouseX, mouseY, (int) (toolips.mapsScale.get() * 100));
            }

            //Book preview
            else if ((focusedSlot.getStack().getItem() == Items.WRITABLE_BOOK
                ||focusedSlot.getStack().getItem() == Items.WRITTEN_BOOK)
                && toolips.previewBooks()) {
                    drawBookPreview(matrices, focusedSlot.getStack(), mouseX, mouseY);
            }
        }
    }

    // Hiding vanilla tooltips
    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void onDrawMouseoverTooltip(MatrixStack matrices, int x, int y, CallbackInfo info) {
        if (focusedSlot != null && !focusedSlot.getStack().isEmpty() && mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            BetterTooltips toolips = Modules.get().get(BetterTooltips.class);

            if (focusedSlot.getStack().getItem() == Items.FILLED_MAP && toolips.previewMaps()) info.cancel();
            else if (toolips.previewBooks() && BetterTooltips.willRenderBookPreview(focusedSlot.getStack()) && !toolips.showVanilla.get()) info.cancel();
            else if ((Utils.hasItems(focusedSlot.getStack())
                    && toolips.previewShulkers()
                    || (focusedSlot.getStack().getItem() == Items.ENDER_CHEST
                    && toolips.previewEChest()))
                    && !toolips.showVanilla.get()) info.cancel();
        }
    }

    private void draw(MatrixStack matrices, DefaultedList<ItemStack> itemStacks, int mouseX, int mouseY, Color color) {
        RenderSystem.disableDepthTest();
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        mouseX += 8;
        mouseY -= 12;

        drawBackground(matrices, mouseX, mouseY, color);

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

        RenderSystem.enableDepthTest();
    }

    private void drawBackground(MatrixStack matrices, int x, int y, Color color) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f);
        RenderSystem.setShaderTexture(0, TEXTURE_CONTAINER_BACKGROUND);
        DrawableHelper.drawTexture(matrices, x, y, 0, 0, 0, 176, 67, 67, 176);
    }

    private void drawMapPreview(MatrixStack matrices, ItemStack stack, int x, int y, int dimensions) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        int y1 = y - 12;
        int y2 = y1 + dimensions;
        int x1 = x + 8;
        int x2 = x1 + dimensions;
        int z = 300;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE_MAP_BACKGROUND);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
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

            matrices.push();
            matrices.translate(x1, y1, z);
            matrices.scale((float) scale, (float) scale, 1);

            VertexConsumerProvider.Immediate consumer = client.getBufferBuilders().getEntityVertexConsumers();
            client.gameRenderer.getMapRenderer().draw(matrices, consumer, FilledMapItem.getMapId(stack), mapState, false, 0xF000F0);

            matrices.pop();
        }
    }


    private void drawBookPreview(MatrixStack matrices, ItemStack stack, int x, int y) {
        float scale = 0.7f * Modules.get().get(BetterTooltips.class).booksScale.get().floatValue();
        Text page;
        NbtCompound tag = stack.getTag();
        if (tag == null) return;
        NbtList ltag = tag.getList("pages", 8);
        if (ltag.size() < 1) return;
        if (stack.getItem() == Items.WRITABLE_BOOK) page = new LiteralText(ltag.getString(0));
        else page = Text.Serializer.fromLenientJson(ltag.getString(0));
        if (page == null) return;

        int y1 = y - 12;
        int y2 = y - 12 + 8;
        int x1 = x - 8;
        int x2 = x + 16;
        int z = 300;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, BookScreen.BOOK_TEXTURE);
        DrawableHelper.drawTexture(matrices, x1, y1, z, 0, 0, (int) (192 * scale), (int) (192 * scale), 179, 179);

        matrices.push();
        matrices.scale(scale, scale, 1f);
        matrices.translate(0, 0, z + 5);

        int offset = 0;
        for (OrderedText line : mc.textRenderer.wrapLines(page, 192 - 48 - 25)) {
            mc.textRenderer.draw(matrices, line, x2 * (1 / scale), (y2 + offset) * (1 / scale), 0x000000);
            offset += 8;
        }

        matrices.pop();
    }

    // Item Highlight
    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void onDrawSlot(MatrixStack matrices, Slot slot, CallbackInfo info) {
        int color = Modules.get().get(ItemHighlight.class).getColor(slot.getStack());
        if (color != -1) fill(matrices, slot.x, slot.y, slot.x + 16, slot.y + 16, color);
    }
}
