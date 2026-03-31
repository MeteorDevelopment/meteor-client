/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.screens.EditBookTitleAndAuthorScreen;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
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

@Mixin(BookViewScreen.class)
public abstract class BookViewScreenMixin extends Screen {
    @Shadow
    private BookViewScreen.BookAccess bookAccess;

    @Shadow
    private int currentPage;

    public BookViewScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        addDrawableChild(
            new Button.Builder(Component.literal("Copy"), button -> {
                ListTag listTag = new NbtList();
                for (int i = 0; i < bookAccess.getPageCount(); i++)
                    listTag.add(StringTag.of(bookAccess.getPage(i).getString()));

                CompoundTag tag = new NbtCompound();
                tag.put("pages", listTag);
                tag.putInt("currentPage", currentPage);

                FastByteArrayOutputStream bytes = new FastByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                try {
                    NbtIo.write(tag, out);
                } catch (IOException e) {
                    e.printStackTrace();
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
        InteractionHand hand = InteractionHand.MAIN_HAND;

        if (itemStack.getItem() != Items.WRITTEN_BOOK) {
            itemStack = mc.player.getOffHandStack();
            hand = InteractionHand.OFF_HAND;
        }
        if (itemStack.getItem() != Items.WRITTEN_BOOK) return;

        ItemStack book = itemStack; // Fuck you Java
        InteractionHand hand2 = hand; // Honestly

        addDrawableChild(
            new Button.Builder(Component.literal("Edit title & author"), button -> {
                mc.setScreen(new EditBookTitleAndAuthorScreen(GuiThemes.get(), book, hand2));
            })
                .position(4, 4 + 20 + 2)
                .size(120, 20)
                .build()
        );
    }
}
