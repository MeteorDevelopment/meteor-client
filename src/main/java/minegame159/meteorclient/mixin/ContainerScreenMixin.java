package minegame159.meteorclient.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.misc.EChestPreview;
import minegame159.meteorclient.utils.EChestMemory;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.container.Slot;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerScreen.class)
public class ContainerScreenMixin {
    @Shadow protected Slot focusedSlot;

    private static final Identifier TEXTURE = new Identifier("meteor-client", "container_3x9.png");
    private static MinecraftClient mc;

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (focusedSlot != null && !focusedSlot.getStack().isEmpty()) {
            // Shulker Preview
            if (Utils.isShulker(focusedSlot.getStack().getItem()) && MeteorClient.INSTANCE.shulkerPeek.isPressed()) {
                CompoundTag compoundTag = focusedSlot.getStack().getSubTag("BlockEntityTag");
                if (compoundTag != null) {
                    if (compoundTag.contains("Items", 9)) {
                        DefaultedList<ItemStack> itemStacks = DefaultedList.ofSize(27, ItemStack.EMPTY);
                        Inventories.fromTag(compoundTag, itemStacks);

                        draw(itemStacks, mouseX, mouseY);
                    }
                }
            }

            // EChest preview
            if (focusedSlot.getStack().getItem() == Items.ENDER_CHEST && ModuleManager.INSTANCE.isActive(EChestPreview.class)) {
                draw(EChestMemory.ITEMS, mouseX, mouseY);
            }
        }
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void onDrawMouseoverTooltip(int mouseX, int mouseY, CallbackInfo info) {
        if (focusedSlot != null && !focusedSlot.getStack().isEmpty()) {
            if (Utils.isShulker(focusedSlot.getStack().getItem()) && MeteorClient.INSTANCE.shulkerPeek.isPressed()) info.cancel();
            else if (focusedSlot.getStack().getItem() == Items.ENDER_CHEST && ModuleManager.INSTANCE.isActive(EChestPreview.class)) info.cancel();
        }
    }

    private void draw(DefaultedList<ItemStack> itemStacks, int mouseX, int mouseY) {
        GlStateManager.disableLighting();
        GlStateManager.disableDepthTest();

        mc = MinecraftClient.getInstance();
        drawBackground(mouseX + 6, mouseY + 6);
        DiffuseLighting.enableForItems();

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

        GlStateManager.enableLighting();
        GlStateManager.enableDepthTest();
    }

    private void drawItem(ItemStack itemStack, int x, int y) {
        mc.getItemRenderer().renderGuiItem(mc.player, itemStack, x, y);
        mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, x, y, null);
    }

    private void drawBackground(int x, int y) {
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEXTURE);
        int width = 176;
        int height = 67;
        DrawableHelper.blit(x, y, 0, 0, 0, width, height, height, width);
    }

}
