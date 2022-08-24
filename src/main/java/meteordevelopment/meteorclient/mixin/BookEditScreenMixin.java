/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
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
    @Shadow @Final private List<String> pages;
    @Shadow private int currentPage;
    @Shadow private boolean dirty;

    public BookEditScreenMixin(Text title) {
        super(title);
    }

    @Shadow protected abstract void updateButtons();

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        addDrawableChild(new ButtonWidget(4, 4, 120, 20, Text.literal("Copy"), button -> {
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
        }));

        addDrawableChild(new ButtonWidget(4, 4 + 20 + 2, 120, 20, Text.literal("Paste"), button -> {
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
                NbtCompound tag = NbtIo.read(in);

                NbtList listTag = tag.getList("pages", 8).copy();

                pages.clear();
                for(int i = 0; i < listTag.size(); ++i) {
                    pages.add(listTag.getString(i));
                }

                if (pages.isEmpty()) {
                    pages.add("");
                }

                currentPage = tag.getInt("currentPage");

                dirty = true;
                updateButtons();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }
}
