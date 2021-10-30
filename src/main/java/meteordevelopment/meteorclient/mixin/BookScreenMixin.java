/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.screens.EditBookTitleAndAuthorScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
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
public class BookScreenMixin extends Screen {
    @Shadow
    private BookScreen.Contents contents;

    @Shadow
    private int pageIndex;

    public BookScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        addDrawableChild(new ButtonWidget(4, 4, 120, 20, new LiteralText("Copy"), button -> {
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
                e.printStackTrace();
            }

            GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), Base64.getEncoder().encodeToString(bytes.array));
        }));

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

        addDrawableChild(new ButtonWidget(4, 4 + 20 + 2, 120, 20, new LiteralText("Edit title & author"), button -> {
            mc.setScreen(new EditBookTitleAndAuthorScreen(GuiThemes.get(), book, hand2));
        }));
    }
}
