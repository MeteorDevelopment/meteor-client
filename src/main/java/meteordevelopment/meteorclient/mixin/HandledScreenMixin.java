/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import meteordevelopment.meteorclient.systems.modules.render.ItemHighlight;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {
    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;

    @Shadow
    @Nullable
    protected abstract Slot getHoveredSlot(double xPosition, double yPosition);

    @Shadow
    public abstract T getMenu();

    @Shadow
    private boolean doubleclick;

    @Shadow
    protected abstract void slotClicked(Slot slot, int invSlot, int clickData, ContainerInput actionType);

    @Shadow
    public abstract void onClose();

    public HandledScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        InventoryTweaks invTweaks = Modules.get().get(InventoryTweaks.class);

        if (invTweaks.isActive() && invTweaks.showButtons() && invTweaks.canSteal(getMenu())) {
            addRenderableWidget(
                new Button.Builder(Component.literal("Steal"), button -> invTweaks.steal(getMenu()))
                    .pos(leftPos, topPos - 22)
                    .size(40, 20)
                    .build()
            );

            addRenderableWidget(
                new Button.Builder(Component.literal("Dump"), button -> invTweaks.dump(getMenu()))
                    .pos(leftPos + 42, topPos - 22)
                    .size(40, 20)
                    .build()
            );
        }
    }

    // Inventory Tweaks
    @Inject(method = "mouseDragged", at = @At("TAIL"))
    private void onMouseDragged(MouseButtonEvent click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != GLFW_MOUSE_BUTTON_LEFT || doubleclick || !Modules.get().get(InventoryTweaks.class).mouseDragItemMove()) return;

        Slot slot = getHoveredSlot(click.x(), click.y());
        if (slot != null && slot.hasItem() && mc.hasShiftDown()) slotClicked(slot, slot.index, click.button(), ContainerInput.QUICK_MOVE);
    }

    // Middle click open
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);

        if (tooltips.shouldOpenContents(click) && hoveredSlot != null && !hoveredSlot.getItem().isEmpty() && getMenu().getCarried().isEmpty()) {
            if (tooltips.openContent(hoveredSlot.getItem())) {
                cir.setReturnValue(true);
            }
        }
    }

    // Keyboard input for middle click open
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void keyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);

        if (tooltips.shouldOpenContents(input) && hoveredSlot != null && !hoveredSlot.getItem().isEmpty() && getMenu().getCarried().isEmpty()) {
            if (tooltips.openContent(hoveredSlot.getItem())) {
                cir.setReturnValue(true);
            }
        }
    }

    // Item Highlight
    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void onDrawSlot(GuiGraphicsExtractor context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        int color = Modules.get().get(ItemHighlight.class).getColor(slot.getItem());
        if (color != -1) context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color);
    }

    @ModifyReturnValue(method = "showTooltipWithItemInHand", at = @At("RETURN"))
    private boolean isTooltipSticky(boolean original, ItemStack item) {
        if (item.getTooltipImage().orElse(null) instanceof ClientTooltipComponent component) {
            return original || component.showTooltipWithItemInHand();
        }

        return original;
    }
}
