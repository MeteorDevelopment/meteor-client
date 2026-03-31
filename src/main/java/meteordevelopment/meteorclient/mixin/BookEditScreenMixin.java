/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    @Shadow
    @Final
    private List<String> pages;
    @Shadow
    private int currentPage;

    @Shadow
    protected abstract void updatePageContent();

    public BookEditScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        addDrawableChild(
            new Button.Builder(Component.literal("Copy"), button -> {
                NbtList listTag = new NbtList();
                pages.stream().map(NbtString::of).forEach(listTag::add);

                NbtCompound tag = new NbtCompound();
                tag.put("pages", listTag);
                tag.putInt("currentPage", currentPage);

                FastByteArrayOutputStream bytes = new FastByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                try {
                    NbtIo.write(tag, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), Base64.getEncoder().encodeToString(bytes.array));
                } catch (OutOfMemoryError exception) {
                    GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), exception.toString());
                }
            })
                .position(4, 4)
                .size(120, 20)
                .build()
        );

        addDrawableChild(
            new Button.Builder(Component.literal("Paste"), button -> {
                String clipboard = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
                if (clipboard == null) return;

                byte[] bytes;
                try {
                    bytes = Base64.getDecoder().decode(clipboard);
                } catch (IllegalArgumentException ignored) {
                    return;
                }
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));

                try {
                    NbtCompound tag = NbtIo.readCompressed(in, NbtSizeTracker.ofUnlimitedBytes());

                    NbtList listTag = tag.getListOrEmpty("pages").copy();

                    pages.clear();
                    for (int i = 0; i < listTag.size(); ++i) {
                        pages.add(listTag.getString(i, ""));
                    }

                    if (pages.isEmpty()) {
                        pages.add("");
                    }

                    currentPage = tag.getInt("currentPage", 0);

                    updatePageContent();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            })
                .position(4, 4 + 20 + 2)
                .size(120, 20)
                .build()
        );
    }
}
