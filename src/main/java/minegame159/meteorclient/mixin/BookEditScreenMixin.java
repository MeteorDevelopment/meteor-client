/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.text.LiteralText;
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

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    @Shadow @Final private List<String> pages;

    @Shadow private int currentPage;

    @Shadow private boolean dirty;

    @Shadow protected abstract void updateButtons();

    public BookEditScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        addButton(new ButtonWidget(4, 4, 70, 16, new LiteralText("Copy"), button -> {
            ListTag listTag = new ListTag();
            pages.stream().map(StringTag::of).forEach(listTag::add);

            CompoundTag tag = new CompoundTag();
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
                GLFW.glfwSetClipboardString(MinecraftClient.getInstance().getWindow().getHandle(), Base64.getEncoder().encodeToString(bytes.array));
            } catch (OutOfMemoryError exception) {
                GLFW.glfwSetClipboardString(MinecraftClient.getInstance().getWindow().getHandle(), exception.toString());
            }
        }));

        addButton(new ButtonWidget(4, 4 + 16 + 4, 70, 16, new LiteralText("Paste"), button -> {
            String clipboard = GLFW.glfwGetClipboardString(MinecraftClient.getInstance().getWindow().getHandle());
            if (clipboard == null) return;

            byte[] bytes;
            try {
                bytes = Base64.getDecoder().decode(clipboard);
            } catch (IllegalArgumentException ignored) {
                return;
            }
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));

            try {
                CompoundTag tag = NbtIo.read(in);

                ListTag listTag = tag.getList("pages", 8).copy();

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
