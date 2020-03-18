package minegame159.meteorclient.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.MeteorClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
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
        if (focusedSlot != null && !focusedSlot.getStack().isEmpty() && focusedSlot.getStack().getItem() == Items.SHULKER_BOX && MeteorClient.INSTANCE.shulkerPeek.isPressed()) {
            CompoundTag compoundTag = focusedSlot.getStack().getSubTag("BlockEntityTag");
            if (compoundTag != null) {
                if (compoundTag.contains("Items", 9)) {
                    DefaultedList<ItemStack> itemStacks = DefaultedList.ofSize(27, ItemStack.EMPTY);
                    Inventories.fromTag(compoundTag, itemStacks);

                    GlStateManager.disableLighting();
                    GlStateManager.disableDepthTest();

                    mc = MinecraftClient.getInstance();
                    drawBackground(mouseX + 6, mouseY + 6);

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
            }
        }
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void onDrawMouseoverTooltip(int mouseX, int mouseY, CallbackInfo info) {
        if (focusedSlot != null && !focusedSlot.getStack().isEmpty() && focusedSlot.getStack().getItem() == Items.SHULKER_BOX && MeteorClient.INSTANCE.shulkerPeek.isPressed()) {
            info.cancel();
        }
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
