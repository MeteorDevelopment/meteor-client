/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.screens.EditBookTitleAndAuthorScreen;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(BookScreen.class)
public abstract class BookScreenMixin extends Screen {
    @Shadow
    private BookScreen.Contents contents;

    @Shadow
    private int pageIndex;

    @Shadow
    protected abstract void goToNextPage();

    @Shadow
    protected abstract void goToPreviousPage();

    public BookScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        addDrawableChild(
            new ButtonWidget.Builder(Text.literal("Copy"), button -> {
                    NbtList listTag = new NbtList();
                    for (int i = 0; i < contents.getPageCount(); i++) listTag.add(NbtString.of(contents.getPage(i).getString()));

                    NbtCompound tag = new NbtCompound();
                    tag.put("pages", listTag);
                    tag.putInt("currentPage", pageIndex);

                    FastByteArrayOutputStream bytes = new FastByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(bytes);
                    try {
                        NbtIo.write(tag, out);
                    } catch (IOException e) {
                        MeteorClient.LOG.error("Error writing the book to the output stream", e);
                    }

                    String encoded = Base64.getEncoder().encodeToString(bytes.array);

                    @SuppressWarnings("resource")
                    long available = MemoryStack.stackGet().getPointer();
                    long size = MemoryUtil.memLengthUTF8(encoded, true);

                    if (size > available) {
                        ChatUtils.error("Could not copy to clipboard: Out of memory.");
                    } else {
                        GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), encoded);
                    }
                })
                .position(4, 4)
                .size(120, 20)
                .build()
        );

        // Edit title & author
        ItemStack itemStack = mc.player.getMainHandStack();
        Hand hand = Hand.MAIN_HAND;

        if (itemStack.getItem() != Items.WRITTEN_BOOK) {
            itemStack = mc.player.getOffHandStack();
            hand = Hand.OFF_HAND;
        }
        if (itemStack.getItem() != Items.WRITTEN_BOOK) return;

        ItemStack book = itemStack; // Fuck you Java
        Hand hand2 = hand; // Honestly

        addDrawableChild(
                new ButtonWidget.Builder(Text.literal("Edit title & author"), button -> {
                    mc.setScreen(new EditBookTitleAndAuthorScreen(GuiThemes.get(), book, hand2));
                })
                .position(4, 4 + 20 + 2)
                .size(120, 20)
                .build()
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        if (verticalAmount < 0) this.goToNextPage();    // scroll down
        else this.goToPreviousPage();                   // scroll up
        return true;
    }
}
